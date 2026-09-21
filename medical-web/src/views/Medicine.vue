<template>
  <div>
    <el-card shadow="never">
      <el-form :inline="true" :model="query">
        <el-form-item label="药品名称">
          <el-input v-model="query.name" placeholder="请输入药品名称" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="query.category" placeholder="如 抗生素" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button type="success" @click="openDialog()">新增药品</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" style="margin-top: 16px">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="code" label="药品编码" width="130" />
        <el-table-column prop="name" label="药品名称" width="200" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="spec" label="规格" width="140" />
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="price" label="单价(元)" width="110" />
        <el-table-column prop="stock" label="库存" width="110">
          <template #default="{ row }">
            <el-tag :type="stockTag(row.stock)" size="small">{{ row.stock }}</el-tag>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑药品' : '新增药品'" width="520px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="药品编码" prop="code">
          <el-input v-model="form.code" placeholder="如 MED001" />
        </el-form-item>
        <el-form-item label="药品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="如 抗生素" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.spec" placeholder="如 0.25g*24片" />
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" placeholder="如 盒" />
        </el-form-item>
        <el-form-item label="单价(元)" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" />
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
import { pageMedicine, saveMedicine, updateMedicine, deleteMedicine } from '../api/basic.js'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formRef = ref()

const query = reactive({ pageNum: 1, pageSize: 10, name: '', category: '' })

const form = reactive({
  id: null,
  code: '',
  name: '',
  category: '',
  spec: '',
  unit: '',
  price: 0,
  stock: 0,
  status: 1
})

const rules = {
  code: [{ required: true, message: '请输入药品编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const stockTag = (stock) => {
  if (stock <= 0) return 'danger'
  if (stock < 20) return 'warning'
  return 'success'
}

const load = async () => {
  loading.value = true
  try {
    const data = await pageMedicine(query)
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const reset = () => {
  query.name = ''
  query.category = ''
  query.pageNum = 1
  load()
}

const openDialog = (row) => {
  if (row) {
    Object.assign(form, row)
  } else {
    Object.assign(form, {
      id: null, code: '', name: '', category: '', spec: '',
      unit: '', price: 0, stock: 0, status: 1
    })
  }
  dialogVisible.value = true
}

const submit = async () => {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateMedicine(form)
      ElMessage.success('修改成功')
    } else {
      await saveMedicine(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确定删除药品「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteMedicine(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
