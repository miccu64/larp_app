package com.example.larp_app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentTransaction
import com.example.larp_app.others.MyPermissions
import com.example.larp_app.services.HubService
import com.example.larp_app.services.IHubCallback
import com.example.larp_app.ui.login.LoginFragment
import com.example.larp_app.ui.login.RegisterFragment


class MainActivity : IHubCallback, AppCompatActivity() {
    //HTTPS nie zadziala na localhoscie - moze na zewnatrz pojdzie? pasobaloby xD
    //w AndroidManifest.xml jest dodana linia i moze nie dzialac cos przez nia
    private lateinit var perms: MyPermissions
    private lateinit var dialog: android.app.AlertDialog

    lateinit var hub: HubService
    private var bound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as HubService.HubBinder
            hub = binder.getService()
            //register this for callbacks from HubService to fragments
            hub.setCallbacks(this@MainActivity)
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //ignores every certificate of SSL!!!!!!!!!!!!!!
        //NukeSSLCerts.nuke()

        perms = MyPermissions(this)
        grantPermissions() //exitProcess(-1)

    }

    override fun onStart() {
        super.onStart()

        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        //addToBackStack() allows to use back button to back to login from register
        ft.replace(R.id.fragment_container, LoginFragment()).addToBackStack(null)
        ft.commit()

        // Bind to LocalService
        Intent(this, HubService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun showRooms() {
        val mobileArray = arrayOf(
            "Android", "IPhone", "WindowsMobile", "Blackberry",
            "WebOS", "Ubuntu", "Windows7", "Max OS X"
        )
        // Begin the transaction
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, RoomFragment(mobileArray))
        ft.commit()
    }

    fun joinJoinedRoom(room: String) {
        hub.joinJoinedRoom(room)
    }

    fun createRoom(name: String, pass: String) {
        hub.createRoom(name, pass)
    }

    fun joinRoom(name:String, pass: String) {
        hub.joinRoom(name, pass)
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            hub.setCallbacks(null)
            unbindService(connection)
            bound = false
        }
    }

    private fun grantPermissions(): Boolean {
        //checks permissions and ask for them if needed
        if (!perms.checkPermissions())
            ActivityCompat.requestPermissions(
                this, arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE), 1337)
        else return true
        return perms.checkPermissions()
    }

    fun login(login: String, pass: String) {
        showDialog("", "Logowanie...")
        hub.login(login, pass)
    }

    fun register(login: String, email: String, pass: String) {
        showDialog("", "Rejestrowanie...")
        hub.register(email, login, pass)
    }

    fun goToRegister(view: View) {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        //addToBackStack() allows to use back button to back to login from register
        ft.replace(R.id.fragment_container, RegisterFragment()).addToBackStack(null)
        ft.commit()
    }

    fun goToLogin(view: View) {
        //go back to login
        onBackPressed()
    }

    override fun goToLogin2() {
        Handler(Looper.getMainLooper()).post {
            onBackPressed()
        }
    }

    override fun showToast(text: String) {
        //looper is needed bcs of asynchronous callback
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, text, Toast.LENGTH_LONG).show()
        }
    }

    override fun loginSuccess() {
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        //addToBackStack() allows to use back button to back to login from register
        ft.replace(R.id.fragment_container, RegisterFragment()).addToBackStack(null)
        ft.commit()
    }

    override fun showDialog(title: String, message: String) {
        Handler(Looper.getMainLooper()).post {
            hideDialog()
            val builder = android.app.AlertDialog.Builder(this)
            builder.setCancelable(true)
            builder.setTitle(title)
            builder.setMessage(message)
            dialog = builder.create()
            dialog.setCancelable(true)
            dialog.show()
        }
    }

    override fun hideDialog() {
        //try for not dismiss not initialized lateinit property
        try {
            dialog.dismiss()
        } catch (e: Exception) { }
    }


}