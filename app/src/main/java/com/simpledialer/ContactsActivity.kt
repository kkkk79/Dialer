package com.simpledialer

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

data class Contact(val name: String, val number: String)

class ContactsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactAdapter
    private val allContacts = mutableListOf<Contact>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        recyclerView = findViewById(R.id.recyclerContacts)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ContactAdapter(mutableListOf()) { contact ->
            val intent = Intent(this, MainActivity::class.java).apply {
                data = Uri.parse("tel:${contact.number}")
            }
            startActivity(intent)
            finish()
        }
        recyclerView.adapter = adapter

        loadContacts()

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadContacts() {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: continue
                val number = it.getString(numberIndex)?.replace(" ", "") ?: continue
                allContacts.add(Contact(name, number))
            }
        }
        adapter.updateList(allContacts)
    }

    private fun filterContacts(query: String) {
        val filtered = if (query.isEmpty()) {
            allContacts
        } else {
            allContacts.filter {
                it.name.contains(query, ignoreCase = true) || it.number.contains(query)
            }
        }
        adapter.updateList(filtered)
    }
}

class ContactAdapter(
    private val contacts: MutableList<Contact>,
    private val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val tvNumber: TextView = view.findViewById(R.id.tvContactNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = contact.name
        holder.tvNumber.text = contact.number
        holder.itemView.setOnClickListener { onClick(contact) }
    }

    override fun getItemCount() = contacts.size

    fun updateList(newList: List<Contact>) {
        contacts.clear()
        contacts.addAll(newList)
        notifyDataSetChanged()
    }
}
