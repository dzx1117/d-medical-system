import request from './request.js'

export const getOverview = () => request.get('/statistics/overview')
export const getDepartmentRanking = () => request.get('/statistics/department-ranking')
export const getDoctorRanking = () => request.get('/statistics/doctor-ranking')
export const getUpcomingTrend = () => request.get('/statistics/upcoming-trend')
export const getTimeSlot = () => request.get('/statistics/time-slot')
