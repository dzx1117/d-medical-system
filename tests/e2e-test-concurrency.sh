#!/bin/bash
# ============================================================
# 并发防超卖验证脚本
#
# 场景：N 个患者同时抢 SLOTS 个号源
# 预期：恰好 SLOTS 人预约成功，数据库 booked_slots 不超过 total_slots
#
# 依赖：后端已启动在 8080 端口
# ============================================================
BASE=${BASE:-http://localhost:8080}
N=${N:-30}          # 并发患者数
SLOTS=${SLOTS:-5}   # 号源数量
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "======================================================"
echo " 并发抢号防超卖测试"
echo " 并发患者=$N   号源数=$SLOTS   目标=$BASE"
echo "======================================================"

# ---------- 准备管理员 ----------
ATOKEN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
if [ -z "$ATOKEN" ] || [ ${#ATOKEN} -lt 20 ]; then
  echo "管理员登录失败，请确认后端已启动且 admin/admin123 可登录"; exit 1
fi

# ---------- 创建一条只有 SLOTS 个号源的排班 ----------
DAY=$(date -d "+17 days" +%Y-%m-%d 2>/dev/null || date -v+17d +%Y-%m-%d)
curl -s --max-time 20 -X POST $BASE/schedule -H "Authorization: Bearer $ATOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"doctorId\":1,\"scheduleDate\":\"$DAY\",\"timeSlot\":3,\"totalSlots\":$SLOTS,\"fee\":50,\"status\":1}" >/dev/null

SCHED_ID=$(curl -s --max-time 20 "$BASE/schedule/available?doctorId=1&scheduleDate=$DAY" \
  -H "Authorization: Bearer $ATOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "号源 ID = $SCHED_ID   日期 = $DAY"
echo ""

# ---------- 准备 N 个患者账号并获取 token ----------
echo "准备 $N 个患者账号..."
: > "$TMP/tokens.txt"
for i in $(seq 1 $N); do
  U="loadtest$i"
  curl -s --max-time 20 -X POST $BASE/auth/register -H "Content-Type: application/json" \
    -d "{\"username\":\"$U\",\"password\":\"123456\",\"realName\":\"LoadTest$i\",\"phone\":\"1350000$(printf '%04d' $i)\"}" >/dev/null
  T=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
    -d "{\"username\":\"$U\",\"password\":\"123456\"}" | sed -E 's/.*"token":"([^"]+)".*/\1/')
  echo "$T" >> "$TMP/tokens.txt"
done
VALID=$(grep -c '^eyJ' "$TMP/tokens.txt")
echo "有效 token: $VALID / $N"
echo ""

# ---------- 并发抢号 ----------
echo "开始并发抢号..."
rm -f "$TMP"/res_*.json
n=0
while read -r t; do
  n=$((n+1))
  ( curl -s --max-time 90 -X POST "$BASE/appointment/create?scheduleId=$SCHED_ID" \
      -H "Authorization: Bearer $t" > "$TMP/res_$n.json" ) &
  # 每 10 个一批，避免 Git Bash 并发连接数上限导致请求丢失
  if [ $((n % 10)) -eq 0 ]; then wait; fi
done < "$TMP/tokens.txt"
wait

# ---------- 统计 ----------
OK=$(grep -l "预约成功" "$TMP"/res_*.json 2>/dev/null | wc -l)
FULL=$(grep -l "已被约满" "$TMP"/res_*.json 2>/dev/null | wc -l)
BUSY=$(grep -l "人数较多" "$TMP"/res_*.json 2>/dev/null | wc -l)
DUP=$(grep -l "重复预约" "$TMP"/res_*.json 2>/dev/null | wc -l)

echo ""
echo "======================================================"
echo " 并发结果"
echo "------------------------------------------------------"
printf "  预约成功        : %s  （预期 %s）\n" "$OK" "$SLOTS"
printf "  提示号源已满    : %s\n" "$FULL"
printf "  提示并发繁忙    : %s\n" "$BUSY"
printf "  提示重复预约    : %s\n" "$DUP"
echo "======================================================"

if [ "$OK" -eq "$SLOTS" ]; then
  echo " 结论：预约成功数恰好等于号源数，未发生超卖"
else
  echo " 结论：预约成功数($OK) 与号源数($SLOTS) 不一致，请检查"
fi
echo "======================================================"
echo ""
echo "提示：可执行以下 SQL 进一步确认数据库状态"
echo "  SELECT total_slots, booked_slots,"
echo "         (SELECT COUNT(*) FROM appointment WHERE schedule_id=$SCHED_ID AND status=0) AS rows_"
echo "  FROM schedule WHERE id=$SCHED_ID;"
