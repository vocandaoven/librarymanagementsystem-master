<template>
    <el-scrollbar height="100%" style="width: 100%; ">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理
            <el-button type="primary"
                       style=" margin-right: 30px; float: right;" @click="storebooksVisible = true">批量导入</el-button>
            <el-button type="primary" v-model="toSearch" :icon="Search"
                       style=" margin-right: 30px; float: right;" @click="searchBookVisible = true" circle />
        </div>
        <p style="padding: 2.5px;margin-left: 40px;"><span style="font-weight: bold;">当前时间：</span>{{ currentTime }}</p>

        <div style="display: flex;flex-wrap: wrap; justify-content: start;">

            <el-table :header-cell-style="{'text-align':'center'}"
                      :cell-style="{'text-align':'center'}" :data="books" style="width: 100%" max-height="580">
                <el-table-column fixed prop="id" label="BookID" width="100" />
                <el-table-column prop="category" label="Category" width="150" />
                <el-table-column prop="title" label="Title" width="200" />
                <el-table-column prop="press" label="Press" width="150" />
                <el-table-column prop="publish_year" label="PublishYear" width="100" />
                <el-table-column prop="author" label="Author" width="150" />
                <el-table-column prop="price" label="Price" width="150" />
                <el-table-column prop="stock" label="Stock" width="150" >
                    <template #default="scope">
                        <div>
                            <span style="margin-left: 1px">{{scope.row.stock}}</span>
                            <el-button
                                size="mini"
                                round
                                class="btn"
                                icon="EditPen"
                                @click="modifyStockVisible = true,modifyStockBook=scope.row.title,toModifyStock.id=scope.row.id,toModifyStock.number=0"
                            >
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column fixed="right" label="Operations" width="300">
                    <template #default="scope">
                        <el-button
                            size="small"
                            type="danger"
                            icon="Delete"
                            @click="this.toRemoveid=scope.row.id,this.toRemove = scope.row.title, this.removeBookVisible = true"
                        >
                        </el-button>
                        <el-button
                            size="small"
                            type="primary"
                            icon="Edit"
                            @click="this.toModifyInfo.id = scope.row.id, this.toModifyInfo.title = scope.row.title,
                this.toModifyInfo.author = scope.row.author, this.toModifyInfo.category = scope.row.category,this.toModifyInfo.press = scope.row.press,
                this.toModifyInfo.publish_year = scope.row.publish_year,this.toModifyInfo.price = scope.row.price,this.toModifyInfo.stock = scope.row.stock,
                this.modifyBookVisible = true" >
                        </el-button>
                        <el-button
                            size="small"
                            type=""
                            @click="borrowBookVisible=true,borrowBookid=scope.row.id,borrowBooktitle=scope.row.title,borrowCardid=''"
                        >
                            借书
                        </el-button>
                        <el-button
                            size="small"
                            type=""
                            @click="returnBookVisible=true,returnBookid=scope.row.id,returnBooktitle=scope.row.title,returnCardid=''"
                        >
                            还书
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
            <el-button class="mt-4" style="width: 100%"
                       @click="newBookInfo.title = '', newBookInfo.author = '',
                newBookInfo.category = '', newBookInfo.press = '',
                newBookInfo.publish_year= '', newBookInfo.price = '',
                 newBookInfo.stock ='0', newBookVisible = true">
                Add Book
            </el-button>

        </div>

        <!-- 修改存量 -->
        <el-dialog v-model="modifyStockVisible" title="修改存量" width="30%" align-center>
            <span>为<span style="font-weight: bold;">{{ modifyStockBook }}</span>增加存量</span>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                增加存量：
                <el-input v-model="toModifyStock.number" style="width: 12.5vw;" clearable
                          @input="toModifyStock.number = toModifyStock.number.replace(/[^\d|\-]/g,'').replace(/^00/g,'0').replace(/^0-/g,'-')"/>
            </div>
            <span>（输入负数即为减少存量）</span>
            <template #footer>
                <span>
                    <el-button @click="modifyStockVisible = false">取消</el-button>
                    <el-button type="primary" @click="modifyStock"
                               :disabled="toModifyStock.number.length === 0 ">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 借书 -->
        <el-dialog v-model="borrowBookVisible" title="借书" width="30%" align-center>
            <span>您将借<span style="font-weight: bold;">{{ borrowBooktitle }}</span></span>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                您的借书证号：
                <el-input v-model="borrowCardid" style="width: 12.5vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button @click="borrowBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="borrow"
                               :disabled="borrowCardid.length === 0 ">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 还书 -->
        <el-dialog v-model="returnBookVisible" title="还书" width="30%" align-center>
            <span>您将还<span style="font-weight: bold;">{{ returnBooktitle }}</span></span>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                您的借书证号：
                <el-input v-model="returnCardid" style="width: 12.5vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button @click="returnBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="returnBook"
                               :disabled="returnCardid.length === 0 ">确定</el-button>
                </span>
            </template>
        </el-dialog>
        <!-- 新建借书证对话框 -->
        <el-dialog v-model="newBookVisible" title="书籍入库" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-input v-model="newBookInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="newBookInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="newBookInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input v-model="newBookInfo.publish_year" style="width: 12.5vw;" clearable
                          @input="newBookInfo.publish_year = newBookInfo.publish_year.replace(/[^\d]/g,'').replace(/^0/g,'')"/>
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="newBookInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model="newBookInfo.price" style="width: 12.5vw;" clearable
                          @input="newBookInfo.price = newBookInfo.price.replace(/[^\d|\.]/g,'').replace(/^00/g,'0').replace(/^\./g,'0.')"/>
            </div>
            <div style="margin-left: 2vw;   font-weight: bold; font-size: 1rem; margin-top: 20px;">
                存量：
                <el-input v-model="newBookInfo.stock" style="width: 12.5vw;" clearable
                          @input="newBookInfo.stock = newBookInfo.stock.replace(/[^\d|\-]/g,'').replace(/^00/g,'0').replace(/^0-/g,'-')"/>
            </div>

            <template #footer>
                <span>
                    <el-button @click="newBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmNewBook"
                               :disabled="newBookInfo.title.length === 0 || newBookInfo.author.length === 0 || 
                               newBookInfo.press.length === 0 || newBookInfo.category.length === 0 ||
                               newBookInfo.publish_year.length === 0 || newBookInfo.price.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 修改信息对话框 -->
        <el-dialog v-model="modifyBookVisible" :title="'修改信息(书籍:ID ' + this.toModifyInfo.title + ')'" width="30%"
                   align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-input v-model="toModifyInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="toModifyInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="toModifyInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：
                <el-input v-model="toModifyInfo.publish_year" style="width: 12.5vw;" clearable
                          @input="toModifyInfo.publish_year = toModifyInfo.publish_year.replace(/[^\d]/g,'').replace(/^0/g,'')"/>
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="toModifyInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model="toModifyInfo.price" style="width: 12.5vw;" clearable
                          @input="toModifyInfo.price = toModifyInfo.price.replace(/[^\d|\.]/g,'').replace(/^00/g,'0').replace(/^\./g,'0.')"/>
            </div>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="modifyBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="ConfirmModifyBook"
                               :disabled="toModifyInfo.title.length === 0 || toModifyInfo.author.length === 0 ||
                               toModifyInfo.press.length === 0 || toModifyInfo.category.length === 0 ||
                               toModifyInfo.publish_year.length === 0 || toModifyInfo.price.length === 0">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 搜索 -->
        <el-dialog v-model="searchBookVisible" title="搜索" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类型：
                <el-input v-model="searchBookInfo.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：
                <el-input v-model="searchBookInfo.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：
                <el-input v-model="searchBookInfo.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份范围：
                <el-input v-model="searchBookInfo.min_year" style="width: 6.0vw;" clearable />
                <span>   ——   </span>
                <el-input v-model="searchBookInfo.max_year" style="width: 6.0vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：
                <el-input v-model="searchBookInfo.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：
                <el-input v-model="searchBookInfo.min_price" style="width: 6.0vw;" clearable />
                <span>   ——   </span>
                <el-input v-model="searchBookInfo.max_price" style="width: 6.0vw;" clearable />
            </div>

            <template #footer>
                <span>
                    <el-button @click="searchBookVisible = false">取消</el-button>
                    <el-button type="primary" @click="QueryBooks">确定</el-button>
                </span>
            </template>
        </el-dialog>
        <!-- 删除借书证对话框 -->
        <el-dialog v-model="removeBookVisible" title="删除书籍" width="30%">
            <span>确定删除<span style="font-weight: bold;">{{ toRemove }}</span>吗？</span>

            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="removeBookVisible = false">取消</el-button>
                    <el-button type="danger" @click="ConfirmRemoveBook" >
                        删除
                    </el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 批量导入 -->

        <el-dialog v-model="storebooksVisible" title="批量导入" width="70%" height="70%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                请按照“类型,书名,出版社,出版年份,作者,价格,存量”的格式分行导入

            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                e.g. Novel,Le Petit Prince,Press-E,2022,Coco,220.24,1
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                <input type="file" @change="handleFileChange">
            </div>
            <template #footer>
                <span>
                    <el-button @click="storebooksVisible = false">取消</el-button>
                    <el-button type="primary" @click="storebooks">确定</el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  data() {
    return {
      currentTime: '',
      books: [{
        id: 1,
        category:'11',
        title:'1111',
        press:'Press-B',
        publish_year:'2024',
        author:'Vocanda',
        price:'114.514',
        stock:'0'
      }],
      Delete,
      Edit,
      Search,
      toSearch: '', // 搜索内容
      toQuery: '',
      text: '',

      storebooksVisible:false,

      searchBookVisible:false,
      searchBookInfo:{
        category:"",
        title:"",
        press:"",
        min_year:"",
        max_year:"",
        author:"",
        min_price:"",
        max_price:""
      },

      removeBookVisible: false,
      toRemove: 0,
      toRemoveid:0,

      modifyStockVisible:false,
      toModifyStock: {
        id: 0,
        number: 0
      } ,
        modifyStockBook:"",

        returnBooktitle:"",
        returnBookid:0,
        returnBookVisible:false,
        returnCardid:"",

      borrowBooktitle:"",
      borrowBookid:0,
      borrowBookVisible:false,
      borrowCardid:"",

      modifyBookVisible:false,
      toModifyInfo: { // 待修改借书证信息
        id:0,
        category:'',
        title:'',
        press:'',
        publish_year:'',
        author:'',
        price:'',
      },

      newBookVisible:false,
      newBookInfo: { // 待修改借书证信息
        category:'',
        title:'',
        press:'',
        publish_year:'',
        author:'',
        price:'',
        stock:'0'
      }

    }
  },
  created(){
    this.getCurrentTime();
    // 每秒更新一次时间
    setInterval(this.getCurrentTime, 1000);
  },
    methods: {
        ConfirmNewBook() {
            // 发出POST请求
            let response =axios.post("/book",
                { // 请求体
                    type: 1,
                    category: this.newBookInfo.category,
                    title: this.newBookInfo.title,
                    press: this.newBookInfo.press,
                    publish_year: this.newBookInfo.publish_year,
                    author: this.newBookInfo.author,
                    price: this.newBookInfo.price,
                    stock: this.newBookInfo.stock
                })
                .then(response => {
                    ElMessage.info(response.data) // 显示消息提醒
                    this.newBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        ConfirmModifyBook() {
            let response =axios.post("/book",
                { // 请求体
                    type: 2,
                    bookID: this.toModifyInfo.id,
                    category:this.toModifyInfo.category,
                    title:this.toModifyInfo.title,
                    press:this.toModifyInfo.press,
                    publish_year:this.toModifyInfo.publish_year,
                    author:this.toModifyInfo.author,
                    price:this.toModifyInfo.price,
                })
                .then(response => {
                    ElMessage.info(response.data) // 显示消息提醒
                    this.modifyBookVisible = false
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        ConfirmRemoveBook() {
            let response =axios.post("/book",
                { // 请求体
                    type: 3,
                    bookID:this.toRemoveid
                })
                .then(response => {
                    ElMessage.info(response.data) // 显示消息提醒
                    this.removeBookVisible = false
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        modifyStock(){
            let response =axios.post("/book",
                { // 请求体
                    type: 4,
                    bookID:this.toModifyStock.id,
                    stock:this.toModifyStock.number
                })
                .then(response => {
                    ElMessage.info(response.data) // 显示消息提醒
                    this.modifyStockVisible = false
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        QueryBooks() {
            this.books = [] // 清空列表
            let response = axios.post('/book',
                { // 请求体
                    type: 5,
                    category:this.searchBookInfo.category,
                    title:this.searchBookInfo.title,
                    press:this.searchBookInfo.press,
                    min_year:this.searchBookInfo.min_year,
                    max_year:this.searchBookInfo.max_year,
                    author:this.searchBookInfo.author,
                    min_price:this.searchBookInfo.min_price,
                    max_price:this.searchBookInfo.max_price,
                }
            ) // 向/card发出GET请求
                .then(
                    response => {
                        let books = response.data // 接收响应负载
                        books.forEach(book => { // 对于每个借书证
                            this.books.push(book) // 将其加入到列表中
                        })
                    })
            this.searchBookVisible=false
        },
        borrow(){
            let response =axios.post("/book",
                { // 请求体
                    type: 6,
                    cardID:this.borrowCardid,
                    bookID:this.borrowBookid,
                    borrowTime:this.currentTime
                })
                .then(response => {
                    this.borrowBookVisible=false,
                        ElMessage.info(response.data) // 显示消息提醒
                    this.newBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks() // 重新查询借书证以刷新页面
                })
        },
        returnBook(){
            let response =axios.post("/book",
                { // 请求体
                    type: 7,
                    cardID:this.returnCardid,
                    bookID:this.returnBookid,
                    returnTime:this.currentTime
                })
                .then(response => {
                    this.returnBookVisible=false,
                        ElMessage.info(response.data) // 显示消息提醒
                    this.newBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks()
                })
        },
        getCurrentTime() {
            const now = new Date();
            const year=now.getFullYear();
            const month=(now.getMonth()+1).toString().padStart(2,'0');
            const day=now.getDate().toString().padStart(2,'0');
            const hours = now.getHours().toString().padStart(2,'0');
            const minutes = now.getMinutes().toString().padStart(2,'0');
            const seconds = now.getSeconds().toString().padStart(2,'0');
            this.currentTime = `${year}.${month}.${day} ${hours}:${minutes}:${seconds}`;
        },
        storebooks(){
            let response =axios.post("/book",
                { // 请求体
                    type: 8,
                    text:this.text,
                })
                .then(response => {
                    this.storebooksVisibleVisible=false,
                        ElMessage.info(response.data) // 显示消息提醒
                    this.newBookVisible = false // 将对话框设置为不可见
                    this.QueryBooks()
                })

        },
        handleFileChange(event) {
            const file = event.target.files[0];
            const reader = new FileReader();
            reader.onload = (e) => {
                const fileContent = e.target.result;
                console.log(fileContent)
                this.text = fileContent
            }
            reader.readAsText(file);
        }
    },
    mounted() { // 当页面被渲染时
        this.QueryBooks() // 查询借书证
    }
}


</script>
<style>
.btn{
    min-width: 0;
    margin-left: 10px;
}
</style>