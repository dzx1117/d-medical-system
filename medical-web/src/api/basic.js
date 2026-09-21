import request from './request.js'

// 科室
export const pageDepartment = (params) => request.get('/department/page', { params })
export const listDepartment = () => request.get('/department/list')
export const saveDepartment = (data) => request.post('/department', data)
export const updateDepartment = (data) => request.put('/department', data)
export const deleteDepartment = (id) => request.delete(`/department/${id}`)

// 医生
export const pageDoctor = (params) => request.get('/doctor/page', { params })
export const listDoctor = (params) => request.get('/doctor/list', { params })
export const saveDoctor = (data) => request.post('/doctor', data)
export const updateDoctor = (data) => request.put('/doctor', data)
export const deleteDoctor = (id) => request.delete(`/doctor/${id}`)

// 排班
export const pageSchedule = (params) => request.get('/schedule/page', { params })
export const listAvailableSchedule = (params) => request.get('/schedule/available', { params })
export const saveSchedule = (data) => request.post('/schedule', data)
export const updateSchedule = (data) => request.put('/schedule', data)
export const deleteSchedule = (id) => request.delete(`/schedule/${id}`)

// 药品
export const pageMedicine = (params) => request.get('/medicine/page', { params })
export const listMedicine = () => request.get('/medicine/list')
export const saveMedicine = (data) => request.post('/medicine', data)
export const updateMedicine = (data) => request.put('/medicine', data)
export const deleteMedicine = (id) => request.delete(`/medicine/${id}`)
