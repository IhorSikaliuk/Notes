package com.example.notes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotesActivity : ComponentActivity() {
    private val noteDatabase = NoteDatabase(FirebaseFirestore.getInstance())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NotesScreen(
                onLogout = { logout() },
                onCreateNote = { navigateToNoteActivity() },
                onEditNote = { noteId -> navigateToNoteActivity(noteId) },
                noteDatabase = noteDatabase
            )
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToNoteActivity(noteId: String? = null) {
        val intent = Intent(this, NoteActivity::class.java)
        noteId?.let { intent.putExtra("NOTE_ID", it) } // Передача noteId, если редактируем
        startActivity(intent)
    }
}

@Composable
fun NotesScreen(
    onLogout: () -> Unit,
    onCreateNote: () -> Unit,
    onEditNote: (String) -> Unit,
    noteDatabase: NoteDatabase
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    val notes = remember { mutableStateListOf<Note>() }

    // Загрузка заметок
    LaunchedEffect(userId) {
        if (userId != null) {
            noteDatabase.getAllNotes(
                userId = userId,
                onSuccess = { fetchedNotes ->
                    notes.clear()
                    // Сортировка по modifiedAt, самый новый вверху
                    notes.addAll(fetchedNotes.sortedByDescending { it.modifiedAt })
                },
                onFailure = { e -> e.printStackTrace() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                actions = {
                    Button(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateNote) {
                Text("+")
            }
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                if (notes.isEmpty()) {
                    Text("No notes available.", modifier = Modifier.padding(16.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(notes) { note ->
                            NoteItem(note = note, onNoteClick = { onEditNote(note.id) })
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun NoteItem(note: Note, onNoteClick: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onNoteClick(note.id) }, // Переход к редактированию заметки
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = note.title, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Modified: ${note.modifiedAt}", style = MaterialTheme.typography.body2)
        }
    }
}


