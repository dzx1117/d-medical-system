#!/bin/bash
# ============================================================
# 端到端业务闭环测试
#
# 覆盖：登录 → 预约挂号 → 确认就诊 → 填写病历 → 开具处方
#       → 查看病历 → 缴费 → 取药 → 状态机与权限校验
#
# 说明：Git Bash 会把命令行参数里的非 ASCII 字符转成 GBK 再传给 curl.exe，
#       因此所有含中文的请求体都通过 --data-binary @文件 传入。
#
# 依赖：后端已启动在 8080 端口
# ============================================================
BASE=${BASE:-http://localhost:8080}
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

PASS=0
FAIL=0
ok()   { echo "  [通过] $1"; PASS=$((PASS+1)); }
bad()  { echo "  [失败] $1"; echo "         响应: $(echo "$2" | head -c 200)"; FAIL=$((FAIL+1)); }
check(){ if echo "$2" | grep -q "$3"; then ok "$1"; else bad "$1" "$2"; fi; }
jstr() { echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
jnum() { echo "$1" | grep -o "\"$2\":[0-9]*"      | head -1 | cut -d: -f2; }

echo "======================================================"
echo " 智慧医院系统 · 端到端业务闭环测试"
echo " 目标: $BASE"
echo "======================================================"

# ---------- 1. 患者登录 ----------
echo ""
echo "[1] 患者登录"
PLOGIN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"patient002","password":"123456"}')
PTOKEN=$(jstr "$PLOGIN" token)
check "患者 patient002 登录成功并取得 JWT" "$PLOGIN" '"code":200'

# ---------- 2. 预约挂号 ----------
echo ""
echo "[2] 查询号源并预约挂号"
ATOKEN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 同一医生 + 同一天 + 同一时段不允许重复建排班，所以逐日往后找一个还能建排班的日期，
# 保证脚本可以反复执行（否则第二次运行会因「已存在排班」而拿不到号源）。
OFFSET=19
while :; do
  DAY=$(date -d "+$OFFSET days" +%Y-%m-%d 2>/dev/null || date -v+"$OFFSET"d +%Y-%m-%d)
  CREATE=$(curl -s --max-time 20 -X POST $BASE/schedule -H "Authorization: Bearer $ATOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"doctorId\":1,\"scheduleDate\":\"$DAY\",\"timeSlot\":2,\"totalSlots\":3,\"fee\":50,\"status\":1}")
  echo "$CREATE" | grep -q '"code":200' && break
  OFFSET=$((OFFSET+1))
  if [ "$OFFSET" -gt 400 ]; then echo "往后一年都排不出空闲日期，请检查后端"; exit 1; fi
done

# 取 id 最大的那条，即刚刚新建的排班：同医生同一天可能残留历史排班，head -1 会取错
SCHED_ID=$(curl -s --max-time 20 "$BASE/schedule/available?doctorId=1&scheduleDate=$DAY" \
  -H "Authorization: Bearer $ATOKEN" | grep -o '"id":[0-9]*' | cut -d: -f2 | sort -n | tail -1)

APPT=$(curl -s --max-time 20 -X POST "$BASE/appointment/create?scheduleId=$SCHED_ID" \
  -H "Authorization: Bearer $PTOKEN")
check "预约挂号成功" "$APPT" '"code":200'
APPT_ID=$(jnum "$APPT" id)
echo "         预约ID=$APPT_ID  号源ID=$SCHED_ID"

# ---------- 3. 医生查看患者 ----------
echo ""
echo "[3] 医生登录并查看患者预约"
DLOGIN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"doctor001","password":"123456"}')
DTOKEN=$(jstr "$DLOGIN" token)
check "医生 doctor001 登录成功" "$DLOGIN" '"code":200'

DAPPT=$(curl -s --max-time 20 "$BASE/appointment/doctor?status=0" -H "Authorization: Bearer $DTOKEN")
check "医生可查询待就诊患者列表" "$DAPPT" '"code":200'
check "患者姓名已脱敏显示" "$DAPPT" '\*'

# ---------- 4. 确认就诊 ----------
echo ""
echo "[4] 医生确认就诊"
COMPLETE=$(curl -s --max-time 20 -X POST "$BASE/appointment/complete/$APPT_ID" \
  -H "Authorization: Bearer $DTOKEN")
check "确认就诊成功" "$COMPLETE" '"code":200'

# ---------- 5. 填写电子病历 ----------
echo ""
echo "[5] 医生填写电子病历（中文经文件传参，避免 GBK 乱码）"
sed -e "s/\"chiefComplaint\"/\"appointmentId\":$APPT_ID,\"visitDate\":\"$DAY\",\"chiefComplaint\"/" \
    "$DIR/payload/record.json" > "$TMP/rec.json"

RECORD=$(curl -s --max-time 20 -X POST $BASE/record -H "Authorization: Bearer $DTOKEN" \
  -H "Content-Type: application/json" --data-binary @"$TMP/rec.json")
check "病历保存成功" "$RECORD" '"code":200'
check "中文诊断内容正确保存" "$RECORD" '慢性胃炎急性发作'
RECORD_ID=$(jnum "$RECORD" id)
echo "         病历ID=$RECORD_ID"

# ---------- 6. 开具处方 ----------
echo ""
echo "[6] 医生开具处方（原子扣减药品库存）"
BEFORE=$(curl -s --max-time 20 $BASE/medicine/1 -H "Authorization: Bearer $DTOKEN" \
  | grep -o '"stock":[0-9]*' | head -1 | cut -d: -f2)
echo "         开方前 药品1 库存 = $BEFORE"

sed -e "s/\"items\"/\"medicalRecordId\":$RECORD_ID,\"items\"/" \
    "$DIR/payload/prescription.json" > "$TMP/rx.json"

RX=$(curl -s --max-time 20 -X POST $BASE/prescription -H "Authorization: Bearer $DTOKEN" \
  -H "Content-Type: application/json" --data-binary @"$TMP/rx.json")
check "处方开具成功" "$RX" '"code":200'
RX_ID=$(jnum "$RX" id)
echo "         处方ID=$RX_ID"

AFTER=$(curl -s --max-time 20 $BASE/medicine/1 -H "Authorization: Bearer $DTOKEN" \
  | grep -o '"stock":[0-9]*' | head -1 | cut -d: -f2)
echo "         开方后 药品1 库存 = $AFTER"
if [ "$AFTER" -lt "$BEFORE" ]; then ok "库存已原子扣减 ($BEFORE → $AFTER)"; else bad "库存未扣减" "$AFTER"; fi

# ---------- 7. 患者查看病历 ----------
echo ""
echo "[7] 患者查看自己的病历"
PMREC=$(curl -s --max-time 20 "$BASE/record/my" -H "Authorization: Bearer $PTOKEN")
check "患者可查询本人病历" "$PMREC" '"code":200'
check "病历含诊断信息" "$PMREC" '慢性胃炎急性发作'

# ---------- 8. 缴费 ----------
echo ""
echo "[8] 患者缴费"
PAY=$(curl -s --max-time 20 -X POST "$BASE/prescription/pay/$RX_ID" -H "Authorization: Bearer $PTOKEN")
check "缴费成功" "$PAY" '"code":200'

# ---------- 9. 取药 ----------
echo ""
echo "[9] 患者取药"
PICK=$(curl -s --max-time 20 -X POST "$BASE/prescription/pickup/$RX_ID" -H "Authorization: Bearer $PTOKEN")
check "取药成功" "$PICK" '"code":200'

# ---------- 10. 状态机与权限 ----------
echo ""
echo "[10] 状态机与权限隔离校验"
AGAIN=$(curl -s --max-time 20 -X POST "$BASE/prescription/pay/$RX_ID" -H "Authorization: Bearer $PTOKEN")
check "已取药后重复缴费被拒绝" "$AGAIN" '无法缴费'

CROSS=$(curl -s --max-time 20 -X POST "$BASE/appointment/cancel/$APPT_ID" -H "Authorization: Bearer $DTOKEN")
check "医生无法取消患者预约" "$CROSS" '"code":500'

NOTMINE=$(curl -s --max-time 20 "$BASE/record/my" -H "Authorization: Bearer $DTOKEN")
check "医生视角看不到患者病历列表" "$NOTMINE" '"total":0'

echo ""
echo "======================================================"
echo " 测试结果: 通过 $PASS 项 / 失败 $FAIL 项"
echo "======================================================"
[ "$FAIL" -eq 0 ]
