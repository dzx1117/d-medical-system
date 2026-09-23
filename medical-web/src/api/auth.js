import request from './request.js'

export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const getUserInfo = () => request.get('/auth/info')
// 通知后端把当前 token 加入黑名单，否则它还能继续用到过期
export const logout = () => request.post('/auth/logout')
