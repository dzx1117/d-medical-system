#!/bin/bash
# ============================================================
# 安全加固测试：退出登录 token 失效 + 挂号接口限流
#
# 覆盖：
#   [一] 退出登录后旧 token 立即失效（Redis 黑名单），新 token 不受影响
#   [二] 挂号接口超过阈值返回 429，窗口过期后自动恢复
#
# 依赖：后端已启动在 8080 端口，且 Redis 可用
# ============================================================
BASE=${BASE:-http://localhost:8080}
RATE_LIMIT=${RATE_LIMIT:-5}      # 与 AppointmentController 上的 @RateLimit 保持一致
WINDOW=${WINDOW:-10}

PASS=0
FAIL=0
ok()   { echo "  [通过] $1"; PASS=$((PASS+1)); }
bad()  { echo "  [失败] $1"; echo "         响应: $(echo "$2" | head -c 200)"; FAIL=$((FAIL+1)); }
check(){ if echo "$2" | grep -q "$3"; then ok "$1"; else bad "$1" "$2"; fi; }
jstr() { echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | cut -d'"' -f4; }
jnum() { echo "$1" | grep -o "\"$2\":[0-9]*"      | head -1 | cut -d: -f2; }
jcode(){ jnum "$1" code; }

echo "======================================================"
echo " 智慧医院系统 · 安全加固测试（黑名单 + 限流）"
echo " 目标: $BASE"
echo "======================================================"

# ---------- 一、退出登录后 token 失效 ----------
echo ""
echo "[一] 退出登录后 token 立即失效"

LOGIN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"patient002","password":"123456"}')
T1=$(jstr "$LOGIN" token)
check "登录成功并取得 token" "$LOGIN" '"code":200'

BEFORE=$(curl -s --max-time 20 "$BASE/appointment/my" -H "Authorization: Bearer $T1")
check "退出前：token 可正常访问业务接口" "$BEFORE" '"code":200'

OUT=$(curl -s --max-time 20 -X POST $BASE/auth/logout -H "Authorization: Bearer $T1")
check "调用退出登录接口成功" "$OUT" '"code":200'

AFTER_BIZ=$(curl -s --max-time 20 "$BASE/appointment/my" -H "Authorization: Bearer $T1")
if [ "$(jcode "$AFTER_BIZ")" = "401" ]; then
  ok "退出后：旧 token 访问业务接口被拒（401）"
else
  bad "退出后：旧 token 仍可访问业务接口（期望 401，实际 $(jcode "$AFTER_BIZ")）" "$AFTER_BIZ"
fi

AFTER_INFO=$(curl -s --max-time 20 "$BASE/auth/info" -H "Authorization: Bearer $T1")
if [ "$(jcode "$AFTER_INFO")" = "401" ]; then
  ok "退出后：旧 token 获取用户信息被拒（401）"
else
  bad "退出后：旧 token 仍能拿到用户信息（期望 401，实际 $(jcode "$AFTER_INFO")）" "$AFTER_INFO"
fi

# 重新登录，确认黑名单只针对那一个 token，没有把该用户整个封掉
RELOGIN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"patient002","password":"123456"}')
T2=$(jstr "$RELOGIN" token)
check "重新登录成功" "$RELOGIN" '"code":200'
check "新 token 不受黑名单影响" \
  "$(curl -s --max-time 20 "$BASE/appointment/my" -H "Authorization: Bearer $T2")" '"code":200'
if [ "$T1" != "$T2" ]; then ok "新旧 token 不同"; else bad "新旧 token 相同" "$T2"; fi

# ---------- 二、挂号接口限流 ----------
echo ""
echo "[二] 挂号接口限流（${RATE_LIMIT} 次 / ${WINDOW} 秒）"

ATOKEN=$(curl -s --max-time 20 -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 造一个全新的号源，避免复用已有预约导致干扰
DAY=$(date -d "+21 days" +%Y-%m-%d 2>/dev/null || date -v+21d +%Y-%m-%d)
curl -s --max-time 20 -X POST $BASE/schedule -H "Authorization: Bearer $ATOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"doctorId\":2,\"scheduleDate\":\"$DAY\",\"timeSlot\":1,\"totalSlots\":50,\"fee\":30,\"status\":1}" >/dev/null

SCHED_ID=$(curl -s --max-time 20 "$BASE/schedule/available?doctorId=2&scheduleDate=$DAY" \
  -H "Authorization: Bearer $ATOKEN" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "         号源ID=$SCHED_ID  规模=${RATE_LIMIT} 次 / ${WINDOW} 秒"

TOTAL=$((RATE_LIMIT + 3))
PASSED=0
BLOCKED=0
for i in $(seq 1 $TOTAL); do
  RESP=$(curl -s --max-time 20 -X POST "$BASE/appointment/create?scheduleId=$SCHED_ID" \
    -H "Authorization: Bearer $T2")
  CODE=$(jcode "$RESP")
  if [ "$CODE" = "429" ]; then
    BLOCKED=$((BLOCKED+1))
    LAST_BLOCKED="$RESP"
  else
    PASSED=$((PASSED+1))
  fi
  printf "         第 %2d 次: code=%s\n" "$i" "${CODE:-?}"
done

echo ""
if [ "$PASSED" = "$RATE_LIMIT" ]; then
  ok "前 $RATE_LIMIT 次全部放行（未被限流）"
else
  bad "前 $RATE_LIMIT 次放行数不符（期望 $RATE_LIMIT，实际 $PASSED）" ""
fi
if [ "$BLOCKED" = "3" ]; then
  ok "超出阈值的 3 次全部返回 429"
else
  bad "超出阈值返回 429 的次数不符（期望 3，实际 $BLOCKED）" "$LAST_BLOCKED"
fi
check "限流提示文案正确" "$LAST_BLOCKED" '操作过于频繁'

# 等窗口过期，确认不是永久封禁
echo "         等待 $((WINDOW+2)) 秒让限流窗口过期…"
sleep $((WINDOW+2))
AFTER_WINDOW=$(curl -s --max-time 20 -X POST "$BASE/appointment/create?scheduleId=$SCHED_ID" \
  -H "Authorization: Bearer $T2")
if [ "$(jcode "$AFTER_WINDOW")" = "429" ]; then
  bad "窗口过期后仍被限流（说明计数没有正确过期）" "$AFTER_WINDOW"
else
  ok "窗口过期后自动恢复，不再返回 429"
fi

echo ""
echo "======================================================"
echo " 通过 $PASS 项，失败 $FAIL 项"
echo "======================================================"
[ "$FAIL" -eq 0 ] || exit 1
