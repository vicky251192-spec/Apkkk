package com.schoolsphere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Student(val name: String, val roll: String, val grade: String, val section: String, val guardian: String)
enum class Page(val label: String) { Students("Students"), Attendance("Attendance"), Academics("Academics"), Portfolio("Portfolio") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SchoolSphereApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolSphereApp() {
    var page by remember { mutableStateOf(Page.Students) }
    val students = remember { mutableStateListOf(
        Student("Aarav Patel", "001", "Class 7", "A", "R. Patel"),
        Student("Diya Shah", "002", "Class 7", "A", "M. Shah"),
        Student("Kabir Desai", "003", "Class 8", "B", "S. Desai")
    ) }
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var roll by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("Class 7") }
    var section by remember { mutableStateOf("A") }
    var guardian by remember { mutableStateOf("") }
    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("SchoolSphere") }, subtitle = { Text("Student Management") }) },
            bottomBar = {
                NavigationBar {
                    Page.values().forEach { item ->
                        NavigationBarItem(selected = page == item, onClick = { page = item },
                            icon = { Text(when(item) { Page.Students -> "👨‍🎓"; Page.Attendance -> "📅"; Page.Academics -> "📚"; Page.Portfolio -> "🏆" }) },
                            label = { Text(item.label) })
                    }
                }
            },
            floatingActionButton = {
                if (page == Page.Students) FloatingActionButton(onClick = { showAdd = true }) { Text("+") }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                Text("Welcome to your school dashboard", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Students", students.size.toString(), Modifier.weight(1f))
                    SummaryCard("Classes", students.map { it.grade }.distinct().size.toString(), Modifier.weight(1f))
                    SummaryCard("Attendance", "Today", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                when(page) {
                    Page.Students -> {
                        Text("Student directory", style = MaterialTheme.typography.titleLarge)
                        LazyColumn { items(students) { s ->
                            Card(Modifier.fillMaxWidth().padding(vertical=5.dp)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(s.name, style=MaterialTheme.typography.titleMedium)
                                    Text("Admission/Roll: ${s.roll}  •  ${s.grade} - ${s.section}")
                                    Text("Guardian: ${s.guardian}")
                                }
                            }
                        } }
                    }
                    Page.Attendance -> FeaturePage("Daily attendance", "Select a class and mark each student Present, Absent or Leave. Attendance records will be connected to secure cloud storage in the next stage.")
                    Page.Academics -> FeaturePage("Academic records", "Maintain subject-wise marks, exams, grades and student performance reports.")
                    Page.Portfolio -> FeaturePage("Activities & achievements", "Build each student's portfolio with sports, arts, leadership, certificates and activity photos.")
                }
            }
        }
        if(showAdd) AlertDialog(
            onDismissRequest = { showAdd=false },
            title = { Text("Add student") },
            text = { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name,{name=it},label={Text("Student name")},singleLine=true)
                OutlinedTextField(roll,{roll=it},label={Text("Admission / roll no.")},singleLine=true)
                OutlinedTextField(grade,{grade=it},label={Text("Class")},singleLine=true)
                OutlinedTextField(section,{section=it},label={Text("Section")},singleLine=true)
                OutlinedTextField(guardian,{guardian=it},label={Text("Parent / guardian")},singleLine=true)
            } },
            confirmButton = { TextButton(onClick={
                if(name.isNotBlank() && roll.isNotBlank()) students.add(Student(name.trim(),roll.trim(),grade.trim(),section.trim(),guardian.trim()))
                name=""; roll=""; guardian=""; showAdd=false
            }) { Text("Save") } },
            dismissButton = { TextButton(onClick={showAdd=false}) { Text("Cancel") } }
        )
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(value, style=MaterialTheme.typography.titleLarge); Text(label, style=MaterialTheme.typography.bodySmall) } }
}
@Composable
fun FeaturePage(title: String, description: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text(title, style=MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(description)
        Spacer(Modifier.height(12.dp))
        Text("Module scaffold ready", color=MaterialTheme.colorScheme.primary)
    } }
}
