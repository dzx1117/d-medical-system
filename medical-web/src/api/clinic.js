import request from './request.js'

// 预约
export const createAppointment = (scheduleId) =>
  request.post('/appointment/create', null, { params: { scheduleId } })
export const cancelAppointment = (id) => request.post(`/appointment/cancel/${id}`)
export const pageMyAppointments = (params) => request.get('/appointment/my', { params })
export const pageDoctorAppointments = (params) => request.get('/appointment/doctor', { params })
export const completeAppointment = (id) => request.post(`/appointment/complete/${id}`)

// 病历
export const pageMyRecords = (params) => request.get('/record/my', { params })
export const pageDoctorRecords = (params) => request.get('/record/doctor', { params })
export const getRecordById = (id) => request.get(`/record/${id}`)
export const getRecordByAppointment = (appointmentId) =>
  request.get(`/record/by-appointment/${appointmentId}`)
export const saveRecord = (data) => request.post('/record', data)
export const updateRecord = (data) => request.put('/record', data)

// 处方
export const createPrescription = (data) => request.post('/prescription', data)
export const pageMyPrescriptions = (params) => request.get('/prescription/my', { params })
export const pageDoctorPrescriptions = (params) => request.get('/prescription/doctor', { params })
export const getPrescriptionById = (id) => request.get(`/prescription/${id}`)
export const payPrescription = (id) => request.post(`/prescription/pay/${id}`)
export const pickupPrescription = (id) => request.post(`/prescription/pickup/${id}`)
