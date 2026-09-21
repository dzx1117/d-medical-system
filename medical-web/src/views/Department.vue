<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="科室名称">
          <el-input v-model="query.deptName" placeholder="请输入科室名称" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="openDialog()">新增科室</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="deptCode" label="科室编码" width="140" />
        <el-table-column prop="deptName" label="科室名称" width="180" />
        <el-table-column prop="location" label="位置" width="160" />
        <el-table-column prop="description" label="科室介绍" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑科室' : '新增科室'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="科室编码" prop="deptCode">
          <el-input v-model="form.deptCode" placeholder="如 DEPT001" />
        </el-form-item>
        <el-form-item label="科室名称" prop="deptName">
          <el-input v-model="form.deptName" placeholder="如 内科" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="如 门诊楼2层" />
        </el-form-item>
        <el-form-item label="科室介绍">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageDepartment, saveDepartment, updateDepartment, deleteDepartment } from '../api/basic.js'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, deptName: '' })

const form = reactive({
  id: null,
  deptCode: '',
  deptName: '',
  location: '',
  description: '',
  status: 1
})

const rules = {
  deptCode: [{ required: true, message: '请输入科室编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageDepartment(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.deptName = ''
  query.pageNum = 1
  load()
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, { id: null, deptCode: '', deptName: '', location: '', description: '', status: 1 })
  }
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateDepartment(form)
      ElMessage.success('修改成功')
    } else {
      await saveDepartment(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除科室「${row.deptName}」吗？`, '提示', { type: 'warning' })
  await deleteDepartment(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
