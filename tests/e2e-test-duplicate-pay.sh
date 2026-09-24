#!/bin/bash
# ============================================================
# 并发重复缴费 · 防重复扣款验证
#
# 场景：同一张「待缴费」处方，N 个请求同时发起缴费
# 预期：恰好 1 次成功，其余全部被状态机拒绝
#
# 为什么单独测这一条：
#   挂号防超卖是靠 Redis 锁 + 数据库原子更新两道防线，缴费这条路径没有加锁，
#   只靠「把状态判断写进 UPDATE 的 WHERE 条件」这一条 SQL 兜底。
#   如果写成「先查状态、再更新」，两个并发请求会双双通过校验各写一次——
#   目前 status 只是状态位，改两次结果一样看不出来；但换成真实支付，
#   这就是同一笔订单被重复扣款。
#
# 说明：脚本会先走一遍完整链路造出一张待缴费处方，因此可以反复执行。
# 依赖：后端已启动在 8080 端口；node 可用时并发效果更好（见下面的说明）
#
# 本脚本的有效性验证（不是"跑通就算数"）：
#   把 UPDATE 的 WHERE 条件去掉（等价于旧的「先查状态再更新」），重跑本脚本，
#   结果是「缴费成功 50 / 50」——脚本会红。改回条件更新后为 1 / 50。
#   一个永远通过的测试没有价值，所以这条反向验证必须做过。
# ============================================================
BASE=${BASE:-http://localhost:8080}
N=${N:-50}          # 并发缴费请求数
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

PASS=0
FAIL=0
ok()   { echo "  [通过] $1"; PASS=$((PASS+1)); }
bad()  { echo "  [失败] $1"; echo "         响应: $(echo "$2" | head -c 200)"; FAIL=$((FAIL+1)); }
idof() { echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2; }

echo "======================================================"
echo " 并发重复缴费 · 防重复扣款测试"
echo " 并发请求=$N   目标=$BASE"
echo "======================================================"

# ---------- 准备：走一遍完整链路，造出一张「待缴费」的处方 ----------
echo ""
echo "[准备] 构造一张待缴费处方"
ATOKEN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
PTOKEN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"patient002","password":"123456"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
DTOKEN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"doctor001","password":"123456"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

if [ -z "$ATOKEN" ] || [ -z "$PTOKEN" ] || [ -z "$DTOKEN" ]; then
  echo " 登录失败，请确认后端已启动且 admin / patient002 / doctor001 可用"; exit 1
fi

# 同一医生 + 同一天 + 同一时段不允许重复建排班，所以逐日往后找一个还建得了排班的
# 空闲日期，保证脚本可以反复执行，不必手工清库。
OFFSET=23
while :; do
  DAY=$(date -d "+$OFFSET days" +%Y-%m-%d 2>/dev/null || date -v+"$OFFSET"d +%Y-%m-%d)
  CREATE=$(curl -s --max-time 20 -X POST $BASE/schedule -H "Authorization: Bearer $ATOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"doctorId\":1,\"scheduleDate\":\"$DAY\",\"timeSlot\":4,\"totalSlots\":5,\"fee\":50,\"status\":1}")
  echo "$CREATE" | grep -q '"code":200' && break
  OFFSET=$((OFFSET+1))
  if [ "$OFFSET" -gt 400 ]; then echo " 往后一年都排不出空闲日期，请检查后端"; exit 1; fi
done

# 取 id 最大的那条，即刚刚新建的排班
SCHED_ID=$(curl -s --max-time 20 "$BASE/schedule/available?doctorId=1&scheduleDate=$DAY" \
  -H "Authorization: Bearer $ATOKEN" | grep -o '"id":[0-9]*' | cut -d: -f2 | sort -n | tail -1)

APPT_ID=$(idof "$(curl -s --max-time 20 -X POST "$BASE/appointment/create?scheduleId=$SCHED_ID" \
  -H "Authorization: Bearer $PTOKEN")")
curl -s --max-time 20 -X POST "$BASE/appointment/complete/$APPT_ID" -H "Authorization: Bearer $DTOKEN" >/dev/null

sed -e "s/\"chiefComplaint\"/\"appointmentId\":$APPT_ID,\"visitDate\":\"$DAY\",\"chiefComplaint\"/" \
    "$DIR/payload/record.json" > "$TMP/rec.json"
RECORD_ID=$(idof "$(curl -s --max-time 20 -X POST $BASE/record -H "Authorization: Bearer $DTOKEN" \
  -H "Content-Type: application/json" --data-binary @"$TMP/rec.json")")

sed -e "s/\"items\"/\"medicalRecordId\":$RECORD_ID,\"items\"/" \
    "$DIR/payload/prescription.json" > "$TMP/rx.json"
RX_ID=$(idof "$(curl -s --max-time 20 -X POST $BASE/prescription -H "Authorization: Bearer $DTOKEN" \
  -H "Content-Type: application/json" --data-binary @"$TMP/rx.json")")

if [ -z "$SCHED_ID" ] || [ -z "$APPT_ID" ] || [ -z "$RECORD_ID" ] || [ -z "$RX_ID" ]; then
  echo " 构造链路失败（号源=$SCHED_ID 预约=$APPT_ID 病历=$RECORD_ID 处方=$RX_ID），请检查后端"; exit 1
fi
echo "         号源=$SCHED_ID  预约=$APPT_ID  病历=$RECORD_ID  处方=$RX_ID（待缴费）"

# 只造数据不缴费：给「用别的方式制造并发」留个入口
#   SETUP_ONLY=1 bash tests/e2e-test-duplicate-pay.sh
if [ -n "$SETUP_ONLY" ]; then
  echo "RX_ID=$RX_ID"
  echo "PTOKEN=$PTOKEN"
  exit 0
fi

# ---------- 并发缴费 ----------
echo ""
echo "[并发] $N 个请求同时缴同一张处方"

if command -v node >/dev/null 2>&1; then
  # 用 Node 发：Promise.all 在同一轮事件循环里把 N 个请求一起写进 socket，
  # 全部返回只要 ~100ms，能真正压到「先读后写」那个毫秒级空隙上。
  echo "       方式: node Promise.all（真并发）"
  RESULT=$(node "$DIR/concurrent-pay.js" "$BASE" "$RX_ID" "$PTOKEN" "$N")
  OK=$(echo "$RESULT" | grep '^OK='     | cut -d= -f2)
  DENIED=$(echo "$RESULT" | grep '^DENIED=' | cut -d= -f2)
  OTHER=$(echo "$RESULT" | grep '^OTHER='  | cut -d= -f2)
  MS=$(echo "$RESULT" | grep '^MS='     | cut -d= -f2)
  RAW=$(echo "$RESULT" | tr '\n' ' ')
else
  # 退化方案：shell 里 fork curl。fork 一个进程要几毫秒，N 个请求会被拉开到
  # 几百毫秒，而竞态窗口只有毫秒级——压不上。实测同一份有缺陷的代码，
  # 这个方式 50 并发也只成功 1 次，看起来"通过"。所以它只能守住不变量，
  # 不能证明竞态不存在，装个 node 再跑才有意义。
  echo "       ⚠️  未找到 node，退回 curl 并发（并发度不足，不能稳定复现竞态）"
  rm -f "$TMP"/res_*.json
  for i in $(seq 1 $N); do
    ( curl -s --max-time 60 -X POST "$BASE/prescription/pay/$RX_ID" \
        -H "Authorization: Bearer $PTOKEN" > "$TMP/res_$i.json" ) &
  done
  wait
  OK=$(grep -l '"code":200'  "$TMP"/res_*.json 2>/dev/null | wc -l | tr -d ' ')
  DENIED=$(grep -l '无法缴费' "$TMP"/res_*.json 2>/dev/null | wc -l | tr -d ' ')
  OTHER=$((N - OK - DENIED))
  MS="-"
  RAW=$(cat "$TMP"/res_*.json 2>/dev/null | head -c 300)
fi

echo ""
echo "======================================================"
echo " 并发结果"
echo "------------------------------------------------------"
printf "  缴费成功        : %s  （预期 1）\n" "$OK"
printf "  提示无法缴费    : %s  （预期 %s）\n" "$DENIED" "$((N-1))"
printf "  其他响应        : %s\n" "$OTHER"
printf "  全部返回耗时    : %s ms\n" "$MS"
echo "======================================================"

OK_ONLY=1
[ "$OK" -eq 1 ] || OK_ONLY=0
if [ "$OK_ONLY" -eq 1 ]; then
  ok "并发下只有 1 次缴费成功，未发生重复扣款"
else
  bad "缴费成功数($OK) 不等于 1，同一张处方被重复扣款" "$RAW"
fi

# ---------- 状态机复核 ----------
echo ""
echo "[复核] 状态机是否停在正确的位置"

AGAIN=$(curl -s --max-time 20 -X POST "$BASE/prescription/pay/$RX_ID" -H "Authorization: Bearer $PTOKEN")
if echo "$AGAIN" | grep -q '无法缴费'; then
  ok "事后单独再缴一次仍被拒绝"
else
  bad "事后缴费未被拒绝，状态可能不对" "$AGAIN"
fi

PICK=$(curl -s --max-time 20 -X POST "$BASE/prescription/pickup/$RX_ID" -H "Authorization: Bearer $PTOKEN")
if echo "$PICK" | grep -q '"code":200'; then
  ok "取药成功，状态机流转正常（待缴费 → 已缴费 → 已取药）"
else
  bad "取药失败，状态可能被并发写坏" "$PICK"
fi

PICK_AGAIN=$(curl -s --max-time 20 -X POST "$BASE/prescription/pickup/$RX_ID" -H "Authorization: Bearer $PTOKEN")
if echo "$PICK_AGAIN" | grep -q '请先完成缴费'; then
  ok "重复取药被拒绝"
else
  bad "重复取药未被拒绝" "$PICK_AGAIN"
fi

echo ""
echo "======================================================"
echo " 通过 $PASS 项，失败 $FAIL 项"
echo "======================================================"
[ "$FAIL" -eq 0 ] || exit 1
