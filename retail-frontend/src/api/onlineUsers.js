import request from './request'

/** 店家：获取当前 Redis 在线用户列表 */
export function getOnlineUsers() {
  return request.get('/store/online-users')
}

/** 店家：踢用户下线 */
export function kickOnlineUser(userId) {
  return request.delete(`/store/online-users/${userId}`)
}
