package com.schoolsphere.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Student(val id:Long,val name:String,val admission:String,val grade:String,val section:String,val guardian:String,val phone:String)
data class Record(val student:String,val field1:String,val field2:String,val field3:String,val field4:String="")
enum class Page(val label:String){Home("Home"),Students("Students"),Attendance("Attendance"),Academics("Academics"),Portfolio("Portfolio")}

class SchoolDb(ctx:Context):SQLiteOpenHelper(ctx,"schoolsphere.db",null,1){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,admission TEXT UNIQUE NOT NULL,grade TEXT NOT NULL,section TEXT,guardian TEXT,phone TEXT)")
  db.execSQL("CREATE TABLE attendance(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER,date TEXT,status TEXT,UNIQUE(student_id,date))")
  db.execSQL("CREATE TABLE academics(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER,subject TEXT,exam TEXT,marks TEXT,max_marks TEXT)")
  db.execSQL("CREATE TABLE portfolio(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER,category TEXT,title TEXT,details TEXT,date TEXT)")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){}
 fun addStudent(v:List<String>){val c=ContentValues().apply{put("name",v[0]);put("admission",v[1]);put("grade",v[2]);put("section",v[3]);put("guardian",v[4]);put("phone",v[5])};writableDatabase.insertOrThrow("students",null,c)}
 fun students():List<Student>{val a=mutableListOf<Student>();readableDatabase.rawQuery("SELECT * FROM students ORDER BY grade,section,name",null).use{c->while(c.moveToNext())a.add(Student(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)?: "",c.getString(5)?: "",c.getString(6)?: ""))};return a}
 fun studentId(admission:String)=students().firstOrNull{it.admission.equals(admission,true)}?.id
 fun addAttendance(id:Long,date:String,status:String){val v=ContentValues().apply{put("student_id",id);put("date",date);put("status",status)};writableDatabase.insertWithOnConflict("attendance",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
 fun attendance(date:String):Map<Long,String>{val a=mutableMapOf<Long,String>();readableDatabase.rawQuery("SELECT student_id,status FROM attendance WHERE date=?",arrayOf(date)).use{c->while(c.moveToNext())a[c.getLong(0)]=c.getString(1)};return a}
 fun addAcademic(id:Long,v:List<String>){val c=ContentValues().apply{put("student_id",id);put("subject",v[1]);put("exam",v[2]);put("marks",v[3]);put("max_marks",v[4])};writableDatabase.insert("academics",null,c)}
 fun academics():List<Record>{val a=mutableListOf<Record>();readableDatabase.rawQuery("SELECT s.name,a.subject,a.exam,a.marks,a.max_marks FROM academics a JOIN students s ON s.id=a.student_id ORDER BY a.id DESC",null).use{c->while(c.moveToNext())a.add(Record(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)))};return a}
 fun addPortfolio(id:Long,v:List<String>,today:String){val c=ContentValues().apply{put("student_id",id);put("category",v[1]);put("title",v[2]);put("details",v[3]);put("date",v[4].ifBlank{today})};writableDatabase.insert("portfolio",null,c)}
 fun portfolio():List<Record>{val a=mutableListOf<Record>();readableDatabase.rawQuery("SELECT s.name,p.category,p.title,p.details,p.date FROM portfolio p JOIN students s ON s.id=p.student_id ORDER BY p.id DESC",null).use{c->while(c.moveToNext())a.add(Record(c.getString(0),c.getString(1),c.getString(2),c.getString(3)?: "",c.getString(4)))};return a}
 fun count(table:String):Int{readableDatabase.rawQuery("SELECT COUNT(*) FROM "+table,null).use{if(it.moveToFirst())return it.getInt(0)};return 0}
}

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SchoolSphereApp(SchoolDb(this))}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SchoolSphereApp(db:SchoolDb){
 var page by remember{mutableStateOf(Page.Home)};var refresh by remember{mutableIntStateOf(0)};var dialog by remember{mutableStateOf("")};var search by remember{mutableStateOf("")}
 val students=remember(refresh){db.students()};val today=remember{SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())}
 val marks=remember(refresh){db.attendance(today)};val academic=remember(refresh){db.academics()};val portfolio=remember(refresh){db.portfolio()}
 MaterialTheme{
  Scaffold(topBar={TopAppBar(title={Text("SchoolSphere")},subtitle={Text("Student Management")})},bottomBar={NavigationBar{Page.values().forEach{p->NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(when(p){Page.Home->"⌂";Page.Students->"♙";Page.Attendance->"✓";Page.Academics->"▤";Page.Portfolio->"★"})},label={Text(p.label)})}}},floatingActionButton={if(page==Page.Students||page==Page.Academics||page==Page.Portfolio)FloatingActionButton(onClick={dialog=when(page){Page.Students->"student";Page.Academics->"academic";Page.Portfolio->"portfolio";else->""}}){Text("+")}}){pad->
   Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)){
    when(page){
     Page.Home->{Text("School overview",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Students",db.count("students").toString(),Modifier.weight(1f));Stat("Today's attendance",marks.size.toString(),Modifier.weight(1f))};Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Results",db.count("academics").toString(),Modifier.weight(1f));Stat("Portfolio",db.count("portfolio").toString(),Modifier.weight(1f))};Spacer(Modifier.height(16.dp));Button(onClick={page=Page.Students},Modifier.fillMaxWidth()){Text("Student profiles")};Button(onClick={page=Page.Attendance},Modifier.fillMaxWidth()){Text("Take attendance")};Button(onClick={page=Page.Academics},Modifier.fillMaxWidth()){Text("Academic results")};Button(onClick={page=Page.Portfolio},Modifier.fillMaxWidth()){Text("Activities & achievements")}}
     Page.Students->{Text("Student profiles",style=MaterialTheme.typography.headlineSmall);OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),label={Text("Search student / class / admission no.")},singleLine=true);val filtered=students.filter{(it.name+" "+it.admission+" "+it.grade+" "+it.section).contains(search,true)};Text(filtered.size.toString()+" students");LazyColumn{items(filtered){s->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(s.name,style=MaterialTheme.typography.titleMedium);Text("Admission: "+s.admission+" • "+s.grade+" "+s.section);Text("Guardian: "+s.guardian);if(s.phone.isNotBlank())Text("Phone: "+s.phone)}}}}}
     Page.Attendance->{Text("Daily attendance",style=MaterialTheme.typography.headlineSmall);Text("Date: "+today);if(students.isEmpty())Hint("Add students before taking attendance.");LazyColumn{items(students){s->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(10.dp)){Text(s.name+" • "+s.grade+" "+s.section,style=MaterialTheme.typography.titleSmall);Text("Status: "+(marks[s.id]?: "Not marked"));Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Present","Absent","Leave").forEach{status->OutlinedButton(onClick={db.addAttendance(s.id,today,status);refresh++},contentPadding=PaddingValues(horizontal=8.dp,vertical=2.dp)){Text(status)}}}}}}}}
     Page.Academics->{Text("Academic records",style=MaterialTheme.typography.headlineSmall);if(academic.isEmpty())Hint("Tap + to add subject-wise marks.");LazyColumn{items(academic){r->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(r.student,style=MaterialTheme.typography.titleMedium);Text(r.field1+" • "+r.field2);Text("Marks: "+r.field3+" / "+r.field4)}}}}}
     Page.Portfolio->{Text("Activities & achievements",style=MaterialTheme.typography.headlineSmall);if(portfolio.isEmpty())Hint("Tap + to add sports, arts, leadership or other achievements.");LazyColumn{items(portfolio){r->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(r.field2,style=MaterialTheme.typography.titleMedium);Text(r.student+" • "+r.field1+" • "+r.field4);if(r.field3.isNotBlank())Text(r.field3)}}}}}
    }
   }
  }
  when(dialog){
   "student"->FormDialog("Add student",listOf("Full name","Admission / roll no.","Class (e.g. Class 7)","Section","Parent / guardian","Parent phone")){v->if(v[0].isNotBlank()&&v[1].isNotBlank()&&v[2].isNotBlank())runCatching{db.addStudent(v);refresh++};dialog=""}{dialog=""}
   "academic"->FormDialog("Add academic result",listOf("Student admission number","Subject","Exam / term","Marks obtained","Maximum marks")){v->val id=db.studentId(v[0]);if(id!=null&&v.drop(1).all{it.isNotBlank()}){db.addAcademic(id,v);refresh++};dialog=""}{dialog=""}
   "portfolio"->FormDialog("Add portfolio entry",listOf("Student admission number","Category","Achievement title","Description","Date (YYYY-MM-DD)")){v->val id=db.studentId(v[0]);if(id!=null&&v[1].isNotBlank()&&v[2].isNotBlank()){db.addPortfolio(id,v,today);refresh++};dialog=""}{dialog=""}
  }
 }
}

@Composable fun FormDialog(title:String,labels:List<String>,save:(List<String>)->Unit,cancel:()->Unit){
 val values=remember(title){mutableStateListOf<String>().apply{repeat(labels.size){add("")}}}
 AlertDialog(onDismissRequest=cancel,title={Text(title)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){labels.forEachIndexed{i,l->OutlinedTextField(values[i],{values[i]=it},label={Text(l)},singleLine=true,modifier=Modifier.fillMaxWidth())}}},confirmButton={TextButton(onClick={save(values.toList())}){Text("Save")}},dismissButton={TextButton(onClick=cancel){Text("Cancel")}})
}
@Composable fun Stat(label:String,value:String,modifier:Modifier=Modifier){Card(modifier){Column(Modifier.padding(12.dp)){Text(value,style=MaterialTheme.typography.headlineMedium);Text(label,style=MaterialTheme.typography.bodySmall)}}}
@Composable fun Hint(message:String){Card(Modifier.fillMaxWidth()){Text(message,Modifier.padding(14.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
