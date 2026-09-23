import { defineStore } from 'pinia'
import { ref } from 'vue'
import { logout as logoutApi } from '../api/auth.js'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  const setToken = (newToken) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUserInfo = (info) => {
    userInfo.value = info
  }

  const clearLocal = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  /**
   * 退出登录：先让后端把 token 拉黑，再清本地。
   *
   * 顺序不能反——token 一旦从 localStorage 里删掉，请求拦截器就带不上
   * Authorization 头，后端也就无从知道该拉黑哪一个 token 了。
   */
  const logout = async () => {
    try {
      await logoutApi()
    } catch (e) {
      // token 已过期或后端不可用时也要让用户退出，本地清理不能被执行失败卡住
    }
    clearLocal()
  }

  return {
    token,
    userInfo,
    setToken,
    setUserInfo,
    logout
  }
})
