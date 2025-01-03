package com.example.notes

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.ui.Alignment


class NoteActivity : ComponentActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())
    private var noteId: String? = null // Переменная для хранения ID заметки

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем noteId, если редактируем заметку
        noteId = intent.getStringExtra("NOTE_ID")

        setContent {
            NoteScreen(
                noteId = noteId,
                onNoteSaved = { title, records ->
                    saveNoteToDatabase(title, records)
                },
                onLoadNote = { onNoteLoaded ->
                    loadNoteFromDatabase(onNoteLoaded)
                },
                onDeleteNote = {
                    deleteNoteFromDatabase()
                }
            )
        }
    }

    private fun saveNoteToDatabase(title: String, records: List<Record>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val note = Note(
            id =  noteId ?: FirebaseFirestore.getInstance().collection("notes").document().id,
            title = title
        )

        noteDatabase.saveNote(
            userId = userId,
            note = note,
            records = records,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Note saved successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NotesActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Error saving note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun loadNoteFromDatabase(onNoteLoaded: (Note, List<Record>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null || noteId == null) {
            return
        }

        noteDatabase.getNoteById(
            userId = userId,
            noteId = noteId!!,
            onSuccess = { note, records ->
                onNoteLoaded(note, records)
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Error loading note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun deleteNoteFromDatabase() {
        val userId = auth.currentUser?.uid
        if (userId == null || noteId == null) {
            Toast.makeText(this, "Cannot delete note", Toast.LENGTH_SHORT).show()
            return
        }

        noteDatabase.deleteNote(
            userId = userId,
            noteId = noteId!!,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Note deleted successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, NotesActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            },
            onFailure = { exception ->
                runOnUiThread {
                    Toast.makeText(this, "Error deleting note: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun NoteScreen(
    noteId: String?,
    onNoteSaved: (String, List<Record>) -> Unit,
    onLoadNote: ((Note, List<Record>) -> Unit) -> Unit,
    onDeleteNote: () -> Unit
) {
    val context = LocalContext.current
    var titleState by remember { mutableStateOf(TextFieldValue()) }
    var records by remember { mutableStateOf(mutableListOf<Record>()) }
    var currentRecordText by remember { mutableStateOf(TextFieldValue()) }
    var isCheckboxMode by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Загрузка заметки, если noteId не null
    LaunchedEffect(noteId) {
        if (noteId != null) {
            onLoadNote { note, loadedRecords ->
                titleState = TextFieldValue(note.title)
                records = loadedRecords.toMutableList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId != null) "Edit Note" else "Create Note") },
                actions = {
                    if (noteId != null) {
                        IconButton(onClick = { onDeleteNote() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Note")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            TextField(
                value = titleState,
                onValueChange = { titleState = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = currentRecordText,
                onValueChange = { currentRecordText = it },
                label = { Text("Content") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(
                    onClick = {
                        if (currentRecordText.text.isNotBlank()) {
                            records.add(
                                Record(
                                    content = currentRecordText.text,
                                    type = if (isCheckboxMode) "checkbox" else "text",

                                )
                            )
                            currentRecordText = TextFieldValue() // Очистить поле ввода
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Record")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { isCheckboxMode = !isCheckboxMode },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isCheckboxMode) "Switch to Text" else "Switch to Checkbox")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (titleState.text.isNotBlank()) {
                        coroutineScope.launch {
                            onNoteSaved(titleState.text, records)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Title cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Note")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Records:", style = MaterialTheme.typography.h6)
            records.forEach { record ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.type == "checkbox") {
                        if (record.isChecked == null)
                            record.isChecked = false;
                        record.isChecked?.let { isChecked ->
                            val isCheckedState = remember { mutableStateOf(record.isChecked ?: false) }
                            Checkbox(
                                checked = isCheckedState.value,
                                onCheckedChange = { isCheckedNew ->
                                    isCheckedState.value = isCheckedNew
                                    record.isChecked = isCheckedNew
                                }
                            )
                        }
                    }
                    Text(
                        text = record.content,
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }
}
