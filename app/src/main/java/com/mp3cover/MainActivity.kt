package com.mp3cover

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.mp3cover.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Mp3FileAdapter
    private val mp3Files = mutableListOf<Mp3FileItem>()

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pickMp3Files()
        } else {
            Toast.makeText(this, "נדרשות הרשאות לגישה לקבצים", Toast.LENGTH_LONG).show()
        }
    }

    // MP3 file picker
    private val mp3PickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val item = Mp3FileItem.fromUri(this, uri)
                if (item != null && mp3Files.none { it.uri == uri }) {
                    mp3Files.add(item)
                }
            }
            adapter.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()

        binding.btnAddFiles.setOnClickListener {
            checkPermissionsAndPick()
        }

        binding.btnAddFilesEmpty.setOnClickListener {
            checkPermissionsAndPick()
        }

        updateEmptyState()
    }

    private fun setupRecyclerView() {
        adapter = Mp3FileAdapter(
            mp3Files,
            onCoverChangeClick = { item, position -> showCoverOptions(item, position) },
            onRemoveClick = { position ->
                mp3Files.removeAt(position)
                adapter.notifyItemRemoved(position)
                updateEmptyState()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun checkPermissionsAndPick() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            pickMp3Files()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    private fun pickMp3Files() {
        mp3PickerLauncher.launch(arrayOf("audio/mpeg", "audio/*"))
    }

    private var currentEditPosition = -1

    // Image picker for cover art
    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && currentEditPosition >= 0) {
            val item = mp3Files[currentEditPosition]
            val success = Mp3FileItem.writeCoverArt(this, item.uri, uri)
            if (success) {
                // Refresh the item
                val refreshed = Mp3FileItem.fromUri(this, item.uri)
                if (refreshed != null) {
                    mp3Files[currentEditPosition] = refreshed
                    adapter.notifyItemChanged(currentEditPosition)
                }
                Toast.makeText(this, "✅ תמונת הכיסוי עודכנה בהצלחה!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ שגיאה בעדכון התמונה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCoverOptions(item: Mp3FileItem, position: Int) {
        currentEditPosition = position
        val options = arrayOf("בחר תמונה מהגלריה", "הסר תמונה קיימת")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("שינוי תמונת כיסוי")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> imagePicker.launch(arrayOf("image/*"))
                    1 -> removeCover(position)
                }
            }
            .show()
    }

    private fun removeCover(position: Int) {
        val item = mp3Files[position]
        val success = Mp3FileItem.removeCoverArt(this, item.uri)
        if (success) {
            val refreshed = Mp3FileItem.fromUri(this, item.uri)
            if (refreshed != null) {
                mp3Files[position] = refreshed
                adapter.notifyItemChanged(position)
            }
            Toast.makeText(this, "✅ תמונת הכיסוי הוסרה", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "❌ שגיאה בהסרת התמונה", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyState() {
        if (mp3Files.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
}
