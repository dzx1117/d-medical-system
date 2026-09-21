import { createRouter, createWebHistory } from 'vue-router'

const Layout = () => import('../components/Layout.vue')

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/',
    component: Layout,
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },

      // 管理员
      { path: 'statistics', name: 'Statistics', component: () => import('../views/Statistics.vue') },
      { path: 'department', name: 'Department', component: () => import('../views/Department.vue') },
      { path: 'doctor', name: 'Doctor', component: () => import('../views/Doctor.vue') },
      { path: 'schedule', name: 'Schedule', component: () => import('../views/Schedule.vue') },
      { path: 'medicine', name: 'Medicine', component: () => import('../views/Medicine.vue') },

      // 患者
      { path: 'book', name: 'Book', component: () => import('../views/BookAppointment.vue') },
      { path: 'my-appointments', name: 'MyAppointments', component: () => import('../views/MyAppointments.vue') },
      { path: 'my-records', name: 'MyRecords', component: () => import('../views/MyRecords.vue') },
      { path: 'my-prescriptions', name: 'MyPrescriptions', component: () => import('../views/MyPrescriptions.vue') },

      // 医生
      { path: 'doctor-appointments', name: 'DoctorAppointments', component: () => import('../views/DoctorAppointments.vue') },
      { path: 'doctor-records', name: 'DoctorRecords', component: () => import('../views/DoctorRecords.vue') },
      { path: 'doctor-prescriptions', name: 'DoctorPrescriptions', component: () => import('../views/DoctorPrescriptions.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 未登录时拦截到登录页
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const whiteList = ['/login', '/register']
  if (!token && !whiteList.includes(to.path)) {
    return '/login'
  }
  if (token && whiteList.includes(to.path)) {
    return '/dashboard'
  }
  return true
})

export default router
