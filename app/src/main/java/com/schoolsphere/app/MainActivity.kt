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

data class Student(val id:Long,val name:String,val admission:String,val grade:String,val section:String,val guardian:String,val phone:String,val gender:String,val dob:String)
data class Record(val student:String,val field1:String,val field2:String,val field3:String,val field4:String="")
data class SchoolClass(val grade:String,val section:String)
enum class Page(val label:String){Home("Home"),Classes("Classes"),ClassDetail("Class Detail"),Students("Students"),Attendance("Attendance"),Academics("Academics"),Portfolio("Portfolio")}

class SchoolDb(ctx:Context):SQLiteOpenHelper(ctx,"schoolsphere.db",null,2){
 override fun onCreate(db:SQLiteDatabase){
  db.execSQL("CREATE TABLE students(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,admission TEXT UNIQUE NOT NULL,grade TEXT NOT NULL,section TEXT,guardian TEXT,phone TEXT,gender TEXT DEFAULT '',dob TEXT DEFAULT '')")
  db.execSQL("CREATE TABLE attendance(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER,date TEXT,status TEXT,UNIQUE(student_id,date))")
  db.execSQL("CREATE TABLE academics(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER,subject TEXT,exam TEXT,marks TEXT,max_marks TEXT)")
  db.execSQL("CREATE TABLE portfolio(id INTEGER PRIMARY KEY AUTOINCREMENT,student_id INTEGER,category TEXT,title TEXT,details TEXT,date TEXT)")
  db.execSQL("CREATE TABLE class_teachers(grade TEXT NOT NULL,section TEXT NOT NULL,teacher TEXT NOT NULL,PRIMARY KEY(grade,section))")
 }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int){
  if(oldVersion<2){db.execSQL("ALTER TABLE students ADD COLUMN gender TEXT DEFAULT ''");db.execSQL("ALTER TABLE students ADD COLUMN dob TEXT DEFAULT ''");db.execSQL("CREATE TABLE IF NOT EXISTS class_teachers(grade TEXT NOT NULL,section TEXT NOT NULL,teacher TEXT NOT NULL,PRIMARY KEY(grade,section))")}
 }
 fun addStudent(v:List<String>){val c=ContentValues().apply{put("name",v[0]);put("admission",v[1]);put("grade",v[2]);put("section",v[3]);put("guardian",v[4]);put("phone",v[5]);put("gender",v[6]);put("dob",v[7])};writableDatabase.insertOrThrow("students",null,c)}
 fun students():List<Student>{val a=mutableListOf<Student>();readableDatabase.rawQuery("SELECT * FROM students ORDER BY grade,section,name",null).use{c->while(c.moveToNext())a.add(Student(c.getLong(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)?: "",c.getString(5)?: "",c.getString(6)?: "",c.getString(7)?: "",c.getString(8)?: ""))};return a}
 fun classes():List<SchoolClass>{val a=mutableListOf<SchoolClass>();for(n in 1..12){val grade="Class $n";val sections=mutableListOf<String>();readableDatabase.rawQuery("SELECT DISTINCT COALESCE(section,'') FROM students WHERE grade=? ORDER BY section",arrayOf(grade)).use{c->while(c.moveToNext())sections.add(c.getString(0))};if(sections.isEmpty())sections.add("");sections.forEach{section->a.add(SchoolClass(grade,section))}};return a}
 fun studentId(admission:String)=students().firstOrNull{it.admission.equals(admission,true)}?.id
 fun addAttendance(id:Long,date:String,status:String){val v=ContentValues().apply{put("student_id",id);put("date",date);put("status",status)};writableDatabase.insertWithOnConflict("attendance",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
 fun attendance(date:String):Map<Long,String>{val a=mutableMapOf<Long,String>();readableDatabase.rawQuery("SELECT student_id,status FROM attendance WHERE date=?",arrayOf(date)).use{c->while(c.moveToNext())a[c.getLong(0)]=c.getString(1)};return a}
 fun attendanceForClass(grade:String,section:String,date:String):Map<Long,String>{val a=mutableMapOf<Long,String>();readableDatabase.rawQuery("SELECT a.student_id,a.status FROM attendance a JOIN students s ON s.id=a.student_id WHERE s.grade=? AND COALESCE(s.section,'')=? AND a.date=?",arrayOf(grade,section,date)).use{c->while(c.moveToNext())a[c.getLong(0)]=c.getString(1)};return a}
 fun saveTeacher(grade:String,section:String,name:String){val v=ContentValues().apply{put("grade",grade);put("section",section);put("teacher",name)};writableDatabase.insertWithOnConflict("class_teachers",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
 fun teacher(grade:String,section:String):String{readableDatabase.rawQuery("SELECT teacher FROM class_teachers WHERE grade=? AND section=?",arrayOf(grade,section)).use{if(it.moveToFirst())return it.getString(0)};return ""}
 fun addAcademic(id:Long,v:List<String>){val c=ContentValues().apply{put("student_id",id);put("subject",v[1]);put("exam",v[2]);put("marks",v[3]);put("max_marks",v[4])};writableDatabase.insert("academics",null,c)}
 fun academics():List<Record>{val a=mutableListOf<Record>();readableDatabase.rawQuery("SELECT s.name,a.subject,a.exam,a.marks,a.max_marks FROM academics a JOIN students s ON s.id=a.student_id ORDER BY a.id DESC",null).use{c->while(c.moveToNext())a.add(Record(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)))};return a}
 fun academicsForClass(grade:String,section:String):List<Record>{val a=mutableListOf<Record>();readableDatabase.rawQuery("SELECT s.name,a.subject,a.exam,a.marks,a.max_marks FROM academics a JOIN students s ON s.id=a.student_id WHERE s.grade=? AND COALESCE(s.section,'')=? ORDER BY a.exam,s.name,a.subject",arrayOf(grade,section)).use{c->while(c.moveToNext())a.add(Record(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)))};return a}
 fun addPortfolio(id:Long,v:List<String>,today:String){val c=ContentValues().apply{put("student_id",id);put("category",v[1]);put("title",v[2]);put("details",v[3]);put("date",v[4].ifBlank{today})};writableDatabase.insert("portfolio",null,c)}
 fun portfolio():List<Record>{val a=mutableListOf<Record>();readableDatabase.rawQuery("SELECT s.name,p.category,p.title,p.details,p.date FROM portfolio p JOIN students s ON s.id=p.student_id ORDER BY p.id DESC",null).use{c->while(c.moveToNext())a.add(Record(c.getString(0),c.getString(1),c.getString(2),c.getString(3)?: "",c.getString(4)))};return a}
 fun count(table:String):Int{readableDatabase.rawQuery("SELECT COUNT(*) FROM "+table,null).use{if(it.moveToFirst())return it.getInt(0)};return 0}
}

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{SchoolSphereApp(SchoolDb(this))}}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun SchoolSphereApp(db:SchoolDb){
 var page by remember{mutableStateOf(Page.Home)}
 var refresh by remember{mutableIntStateOf(0)}
 var dialog by remember{mutableStateOf("")}
 var search by remember{mutableStateOf("")}
 var selectedGrade by remember{mutableStateOf("")}
 var selectedSection by remember{mutableStateOf("")}
 val students=remember(refresh){db.students()}
 val classes=remember(refresh){db.classes()}
 val today=remember{SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())}
 val marks=remember(refresh){db.attendance(today)}
 val academic=remember(refresh){db.academics()}
 val portfolio=remember(refresh){db.portfolio()}
 val classStudents=students.filter{it.grade==selectedGrade&&it.section==selectedSection}
 val classMarks=remember(refresh,selectedGrade,selectedSection){db.attendanceForClass(selectedGrade,selectedSection,today)}
 val classAcademic=remember(refresh,selectedGrade,selectedSection){db.academicsForClass(selectedGrade,selectedSection)}
 MaterialTheme{
  Scaffold(topBar={TopAppBar(navigationIcon={Surface(color=MaterialTheme.colorScheme.primary,shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),modifier=Modifier.padding(start=12.dp).size(42.dp)){Box(contentAlignment=androidx.compose.ui.Alignment.Center){Text("DJ",color=MaterialTheme.colorScheme.onPrimary,style=MaterialTheme.typography.titleMedium)}}},title={Column{Text("Divya Jyoti School",style=MaterialTheme.typography.titleLarge);Text("ENGLISH MEDIUM  •  CLASSES 1–12",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)}})},
   bottomBar={NavigationBar{Page.values().filter{it!=Page.ClassDetail}.forEach{p->NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(when(p){Page.Home->"⌂";Page.Classes->"▦";Page.ClassDetail->"▣";Page.Students->"♙";Page.Attendance->"✓";Page.Academics->"▤";Page.Portfolio->"★"})},label={Text(p.label)})}}},
   floatingActionButton={if(page==Page.Students||page==Page.Academics||page==Page.Portfolio)FloatingActionButton(onClick={dialog=when(page){Page.Students->"student";Page.Academics->"academic";Page.Portfolio->"portfolio";else->""}}){Text("+")}}){pad->
   Column(Modifier.padding(pad).fillMaxSize().padding(16.dp)){
    when(page){
     Page.Home->{
      Text("Principal Dashboard",style=MaterialTheme.typography.headlineSmall)
      Text("Divya Jyoti School • Your school at a glance",color=MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Total students",db.count("students").toString(),Modifier.weight(1f));Stat("Classes","12",Modifier.weight(1f))}
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Present today",marks.values.count{it=="Present"}.toString(),Modifier.weight(1f));Stat("Absent today",marks.values.count{it=="Absent"}.toString(),Modifier.weight(1f))}
      val boysTotal=students.count{it.gender.equals("Male",true)}
      val girlsTotal=students.count{it.gender.equals("Female",true)}
      val markedToday=marks.values.count{it=="Present"||it=="Absent"||it=="Leave"}
      val attendancePct=if(students.isEmpty())0 else marks.values.count{it=="Present"}*100/students.size
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Boys",boysTotal.toString(),Modifier.weight(1f));Stat("Girls",girlsTotal.toString(),Modifier.weight(1f));Stat("Not marked",(students.size-markedToday).coerceAtLeast(0).toString(),Modifier.weight(1f))}
      Card(Modifier.fillMaxWidth()){Column(Modifier.padding(14.dp)){Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()){Text("School attendance today",style=MaterialTheme.typography.titleSmall);Text("$attendancePct%",style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)};LinearProgressIndicator(progress={attendancePct/100f},modifier=Modifier.fillMaxWidth().padding(top=8.dp));Text("$markedToday of ${students.size} students have an attendance status recorded",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}}
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Stat("Exam records",db.count("academics").toString(),Modifier.weight(1f));Stat("Portfolio",db.count("portfolio").toString(),Modifier.weight(1f))}
      Spacer(Modifier.height(16.dp))
      Button(onClick={page=Page.Classes},Modifier.fillMaxWidth()){Text("Explore class-wise dashboards")}
      OutlinedButton(onClick={page=Page.Attendance},Modifier.fillMaxWidth()){Text("Manage daily attendance")}
      OutlinedButton(onClick={page=Page.Students},Modifier.fillMaxWidth()){Text("Open student directory")}
     }
     Page.Classes->{
      Text("Class-wise Overview",style=MaterialTheme.typography.headlineSmall)
      Text("All 12 classes are ready. Select any class to view its students, attendance, teacher and exam records.",color=MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.height(12.dp))
      
      LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(classes){cl->
       val roster=students.filter{it.grade==cl.grade&&it.section==cl.section}
       val classPresent=db.attendanceForClass(cl.grade,cl.section,today).values.count{it=="Present"}
       val classAbsent=db.attendanceForClass(cl.grade,cl.section,today).values.count{it=="Absent"}
       Card(onClick={selectedGrade=cl.grade;selectedSection=cl.section;page=Page.ClassDetail},modifier=Modifier.fillMaxWidth()){
        Column(Modifier.padding(16.dp)){
         Text(cl.grade+(if(cl.section.isNotBlank())" • Section "+cl.section else ""),style=MaterialTheme.typography.titleLarge)
         Spacer(Modifier.height(6.dp))
         Row(horizontalArrangement=Arrangement.spacedBy(14.dp)){Text("Students: ${roster.size}");Text("Boys: ${roster.count{it.gender.equals("Male",true)}}");Text("Girls: ${roster.count{it.gender.equals("Female",true)}}")}
         Text("Present today: $classPresent   •   Absent: $classAbsent",color=MaterialTheme.colorScheme.primary)
         Text("Class teacher: "+(db.teacher(cl.grade,cl.section).ifBlank{"Not assigned"}),style=MaterialTheme.typography.bodySmall)
        }
       }
      }}
     }
     Page.ClassDetail->{
      Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()){
       Column(Modifier.weight(1f)){Text(selectedGrade+(if(selectedSection.isNotBlank())" • Section $selectedSection" else ""),style=MaterialTheme.typography.headlineSmall);Text("Class dashboard",color=MaterialTheme.colorScheme.onSurfaceVariant)}
       TextButton(onClick={page=Page.Classes}){Text("All classes")}
      }
      val present=classMarks.values.count{it=="Present"}
      val absent=classMarks.values.count{it=="Absent"}
      val leave=classMarks.values.count{it=="Leave"}
      val boys=classStudents.count{it.gender.equals("Male",true)}
      val girls=classStudents.count{it.gender.equals("Female",true)}
      val pct=if(classStudents.isEmpty())0 else (present*100/classStudents.size)
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Stat("Students",classStudents.size.toString(),Modifier.weight(1f));Stat("Boys",boys.toString(),Modifier.weight(1f));Stat("Girls",girls.toString(),Modifier.weight(1f))}
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Stat("Present",present.toString(),Modifier.weight(1f));Stat("Absent",absent.toString(),Modifier.weight(1f));Stat("Leave",leave.toString(),Modifier.weight(1f))}
      Text("Today's attendance: $pct%  •  $today",style=MaterialTheme.typography.titleSmall)
      Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("Class teacher: "+db.teacher(selectedGrade,selectedSection),Modifier.weight(1f));TextButton(onClick={dialog="teacher"}){Text("Assign / Edit")}}
      Divider()
      Text("Student register",style=MaterialTheme.typography.titleLarge)
      if(classStudents.isEmpty())Hint("No students in this class yet. Add a student from Student Profiles.")
      LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){
       items(classStudents){s->
        Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
         Text(s.name,style=MaterialTheme.typography.titleMedium)
         Text("Admission No: ${s.admission}  •  ${s.gender.ifBlank{"Gender not set"}}")
         Text("Date of birth: ${s.dob.ifBlank{"Not added"}}")
         Text("Attendance today: ${classMarks[s.id]?: "Not marked"}")
         val results=classAcademic.filter{it.student==s.name}
         if(results.isNotEmpty()){Text("Exam records",style=MaterialTheme.typography.labelLarge);results.forEach{r->Text("${r.field2} • ${r.field1}: ${r.field3}/${r.field4}",style=MaterialTheme.typography.bodySmall)}}
        }}
       }
      }
      Text("Tip: Use Academics to add subject-wise marks and they will appear here.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     }
     Page.Students->{
      Text("Student Directory",style=MaterialTheme.typography.headlineSmall)
      OutlinedTextField(search,{search=it},Modifier.fillMaxWidth(),label={Text("Search student / class / admission no.")},singleLine=true)
      val filtered=students.filter{(it.name+" "+it.admission+" "+it.grade+" "+it.section).contains(search,true)}
      Text(filtered.size.toString()+" students")
      LazyColumn{items(filtered){s->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(s.name,style=MaterialTheme.typography.titleMedium);Text("Admission: "+s.admission+" • "+s.grade+" "+s.section);Text("Gender: "+s.gender+" • DOB: "+s.dob);Text("Guardian: "+s.guardian);if(s.phone.isNotBlank())Text("Phone: "+s.phone)}}}}
     }
     Page.Attendance->{
      Text("Attendance Register",style=MaterialTheme.typography.headlineSmall);Text("Date: "+today)
      if(students.isEmpty())Hint("Add students before taking attendance.")
      LazyColumn{items(students){s->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(10.dp)){Text(s.name+" • "+s.grade+" "+s.section,style=MaterialTheme.typography.titleSmall);Text("Status: "+(marks[s.id]?: "Not marked"));Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Present","Absent","Leave").forEach{status->OutlinedButton(onClick={db.addAttendance(s.id,today,status);refresh++},contentPadding=PaddingValues(horizontal=8.dp,vertical=2.dp)){Text(status)}}}}}}}
     }
     Page.Academics->{
      Text("Exams & Academic Performance",style=MaterialTheme.typography.headlineSmall)
      if(academic.isEmpty())Hint("Tap + to add subject-wise marks.")
      LazyColumn{items(academic){r->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(r.student,style=MaterialTheme.typography.titleMedium);Text(r.field1+" • "+r.field2);Text("Marks: "+r.field3+" / "+r.field4)}}}}
     }
     Page.Portfolio->{
      Text("Student Activities & Achievements",style=MaterialTheme.typography.headlineSmall)
      if(portfolio.isEmpty())Hint("Tap + to add sports, arts, leadership or other achievements.")
      LazyColumn{items(portfolio){r->Card(Modifier.fillMaxWidth().padding(vertical=4.dp)){Column(Modifier.padding(12.dp)){Text(r.field2,style=MaterialTheme.typography.titleMedium);Text(r.student+" • "+r.field1+" • "+r.field4);if(r.field3.isNotBlank())Text(r.field3)}}}}
     }
    }
   }
  }
  when(dialog){
   "student"->FormDialog(title="Add student",labels=listOf("Full name","Admission / roll no.","Class (Class 1 to Class 12)","Section","Parent / guardian","Parent phone","Gender (Male / Female / Other)","Date of birth (DD-MM-YYYY)"),save={v->if(v[0].isNotBlank()&&v[1].isNotBlank()&&v[2].isNotBlank())runCatching{db.addStudent(v);refresh++};dialog=""},cancel={dialog=""})
   "teacher"->FormDialog(title="Class teacher",labels=listOf("Class: $selectedGrade","Section: $selectedSection","Class teacher full name"),initial=listOf(selectedGrade,selectedSection,db.teacher(selectedGrade,selectedSection)),save={v->if(v[2].isNotBlank()){db.saveTeacher(selectedGrade,selectedSection,v[2].trim());refresh++};dialog=""},cancel={dialog=""})
   "academic"->FormDialog(title="Add academic result",labels=listOf("Student admission number","Subject","Exam / term","Marks obtained","Maximum marks"),save={v->val sid=db.studentId(v[0]);if(sid!=null&&v.drop(1).all{value->value.isNotBlank()}){db.addAcademic(sid,v);refresh++};dialog=""},cancel={dialog=""})
   "portfolio"->FormDialog(title="Add portfolio entry",labels=listOf("Student admission number","Category","Achievement title","Description","Date (YYYY-MM-DD)"),save={v->val sid=db.studentId(v[0]);if(sid!=null&&v[1].isNotBlank()&&v[2].isNotBlank()){db.addPortfolio(sid,v,today);refresh++};dialog=""},cancel={dialog=""})
  }
 }
}

@Composable fun FormDialog(title:String,labels:List<String>,initial:List<String> = emptyList(),save:(List<String>)->Unit,cancel:()->Unit){
 val values=remember(title){mutableStateListOf<String>().apply{labels.indices.forEach{i->add(initial.getOrNull(i)?: "")}}}
 AlertDialog(onDismissRequest=cancel,title={Text(title)},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)){labels.forEachIndexed{i,l->OutlinedTextField(values[i],{values[i]=it},label={Text(l)},singleLine=true,modifier=Modifier.fillMaxWidth())}}},confirmButton={TextButton(onClick={save(values.toList())}){Text("Save")}},dismissButton={TextButton(onClick=cancel){Text("Cancel")}})
}
@Composable fun Stat(label:String,value:String,modifier:Modifier=Modifier){Card(modifier){Column(Modifier.padding(12.dp)){Text(value,style=MaterialTheme.typography.headlineMedium);Text(label,style=MaterialTheme.typography.bodySmall)}}}
@Composable fun Hint(message:String){Card(Modifier.fillMaxWidth()){Text(message,Modifier.padding(14.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}}
