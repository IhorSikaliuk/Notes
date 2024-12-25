package com.example.notes

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Date

class NoteDatabase(private val firestore: FirebaseFirestore) {

    // Получение всех заметок пользователя
    fun getAllNotes(
        userId: String,
        onSuccess: (List<Note>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        firestore.collection("users")
            .document(userId)
            .collection("notes")
            .get()
            .addOnSuccessListener { snapshot ->
                val notes = snapshot.documents.map { doc ->
                    Note(
                        id = doc.id,
                        title = doc.getString("title") ?: "Untitled",
                        modifiedAt = doc.getTimestamp("modifiedAt")?.toDate() ?: Date()
                    )
                }
                onSuccess(notes)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Получение одной заметки по ID
    fun getNoteById(
        userId: String,
        noteId: String,
        onSuccess: (Note, List<Record>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        noteRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val note = Note(
                        id = document.id,
                        title = document.getString("title") ?: "Untitled",
                        modifiedAt = document.getTimestamp("modifiedAt")?.toDate() ?: Date()
                    )

                    // Получаем записи заметки
                    noteRef.collection("records")
                        .get()
                        .addOnSuccessListener { recordsSnapshot ->
                            val records = parseRecords(recordsSnapshot)
                            onSuccess(note, records)
                        }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    onFailure(Exception("Note not found"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Сохранение новой заметки или обновление существующей
    fun saveNote(
        userId: String,
        note: Note,
        records: List<Record>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteRef = if (note.id.isNotEmpty()) {
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document(note.id)
        } else {
            firestore.collection("users")
                .document(userId)
                .collection("notes")
                .document()
        }

        val noteData = mapOf(
            "title" to note.title,
            "modifiedAt" to FieldValue.serverTimestamp() // Используем серверное время
        )

        noteRef.set(noteData)
            .addOnSuccessListener {
                // Очистка старых записей и добавление новых
                noteRef.collection("records").get()
                    .addOnSuccessListener { snapshot ->
                        snapshot.documents.forEach { it.reference.delete() }
                        records.forEachIndexed { index, record ->
                            val recordData = mapOf(
                                "content" to record.content,
                                "type" to record.type,
                                "is_checked" to (if (record.type == "checkbox") false else null),
                                "order" to index
                            )
                            noteRef.collection("records").add(recordData)
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // Удаление заметки по ID
    fun deleteNote(
        userId: String,
        noteId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val noteRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)

        noteRef.collection("records").get()
            .addOnSuccessListener { snapshot ->
                val deleteTasks = snapshot.documents.map { it.reference.delete() }
                Tasks.whenAll(deleteTasks)
                    .addOnSuccessListener {
                        noteRef.delete()
                            .addOnSuccessListener { onSuccess() }
                            .addOnFailureListener { exception -> onFailure(exception) }
                    }
                    .addOnFailureListener { exception -> onFailure(exception) }
            }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

    // Парсинг записей из QuerySnapshot
    private fun parseRecords(snapshot: QuerySnapshot): List<Record> {
        return snapshot.documents.map { document ->
            Record(
                content = document.getString("content") ?: "",
                type = document.getString("type") ?: "text",
                isChecked = document.getBoolean("is_checked"),
                order = document.getLong("order")?.toInt() ?: 0
            )
        }.sortedBy { it.order }
    }

    fun updateRecord(
        userId: String,
        noteId: String,
        recordId: String,
        updatedRecord: Record,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val recordRef = firestore.collection("users")
            .document(userId)
            .collection("notes")
            .document(noteId)
            .collection("records")
            .document(recordId)

        val updatedData = mapOf(
            "content" to updatedRecord.content,
            "type" to updatedRecord.type,
            "is_checked" to updatedRecord.isChecked,
            "order" to updatedRecord.order // Оставляем порядок без изменений
        )

        recordRef.set(updatedData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception -> onFailure(exception) }
    }

}
