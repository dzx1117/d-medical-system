// 并发缴费压测：同时向同一张处方发起 N 个缴费请求，输出统计结果。
// 由 e2e-test-duplicate-pay.sh 调用，也可以手动跑：
//   node concurrent-pay.js <BASE> <处方ID> <TOKEN> <并发数>
//
// 为什么不用 shell 里 fork curl：
//   fork 一个 curl 进程要几毫秒，N 个请求会被拉开到几百毫秒；而「先读后写」
//   的竞态窗口只有毫秒级，压不上去。实测同一份有缺陷的代码（先查状态再无条件
//   更新）：shell 50 并发只有 1 次成功，看起来完全正常；换成本脚本的
//   Promise.all 同时发出，才复现出 10 次重复缴费。
//   所以「并发测试没测出问题」不等于「没有问题」——先确认压得够不够狠。

const [BASE, RX_ID, TOKEN, N_RAW] = process.argv.slice(2);
const N = Number(N_RAW || 50);

if (!BASE || !RX_ID || !TOKEN) {
  console.error('用法: node concurrent-pay.js <BASE> <处方ID> <TOKEN> <并发数>');
  process.exit(2);
}

const fire = () =>
  fetch(`${BASE}/prescription/pay/${RX_ID}`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${TOKEN}` },
  })
    .then((r) => r.json())
    .catch((e) => ({ message: '网络错误: ' + e }));

(async () => {
  const t0 = Date.now();
  // Array.from 一次性造出全部 Promise，同一轮事件循环里一起发出去
  const results = await Promise.all(Array.from({ length: N }, fire));
  const ms = Date.now() - t0;

  const ok = results.filter((r) => r.code === 200).length;
  const denied = results.filter((r) => (r.message || '').includes('无法缴费')).length;
  const other = results.length - ok - denied;

  // 机器可读的输出，给 shell 脚本解析
  console.log(`OK=${ok}`);
  console.log(`DENIED=${denied}`);
  console.log(`OTHER=${other}`);
  console.log(`MS=${ms}`);
})();
