package jp.techacademy.shoutarou.taskapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.inputmethodservice.Keyboard
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.util.Log
import io.realm.Realm
import io.realm.RealmChangeListener
import io.realm.Sort
import kotlinx.android.synthetic.main.activity_main.*
import android.text.TextWatcher
import io.realm.RealmResults


const val EXTRA_TASK = "jp.techacademy.shoutarou.taskapp.TASK"

class MainActivity : AppCompatActivity() {

    private lateinit var mRealm: Realm
    private val mRealmListener = object : RealmChangeListener<Realm> {
        override fun onChange(t: Realm) {
            reloadListView()
        }
    }

    private lateinit var mTaskAdapter: TaskAdapter


    override fun onStart() {
        super.onStart()

        search_edit_text.clearFocus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fab.setOnClickListener { view ->
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            startActivity(intent)
        }

        // クリアボタンの設定
        clear_button.setOnClickListener {view ->

            Log.d("DEBUG","Clear button clicked!!")

            search_edit_text.text.clear()
            search_edit_text.clearFocus()
        }

        search_edit_text.addTextChangedListener(object : TextWatcher {

            // 検索欄内の文字の変更を検知する
            override fun afterTextChanged(s: Editable) {
                Log.d("DEBUG", "Text changed!")
                Log.d("DEBUG","${search_edit_text.text}")

                // 文字の変更のたびに検索する
                if (s.isEmpty()) {

                    reloadListView()

                } else {
                    Log.d("DEBUG","${search_edit_text.text}")

                    // 入力された文字列を使って検索した結果をlistに表示
                    reloadListView(search_edit_text.text.toString())
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int,
                                       before: Int, count: Int) {
            }
        })

        // Realmの設定
        mRealm = Realm.getDefaultInstance()
        mRealm.addChangeListener(mRealmListener)

        // ListViewの設定
        mTaskAdapter = TaskAdapter(this@MainActivity)

        // ListViewのitemをタップしたときの処理
        listView1.setOnItemClickListener { parent, view, position, id ->
            // 入力・編集する画面に遷移させる
            val task = parent.adapter.getItem(position) as Task
            val intent = Intent(this@MainActivity, InputActivity::class.java)
            intent.putExtra(EXTRA_TASK, task.id)
            startActivity(intent)
        }

        // ListViewのitemを長押ししたときの処理
        listView1.setOnItemLongClickListener { parent, view, position, id ->
            // タスクを削除する
            val task = parent.adapter.getItem(position) as Task

            // ダイアログを表示する
            val builder = AlertDialog.Builder(this@MainActivity)

            builder.setTitle("削除")
            builder.setMessage(task.title + "を削除しますか")

            builder.setPositiveButton("OK"){_, _ ->
                val results = mRealm.where(Task::class.java).equalTo("id", task.id).findAll()

                mRealm.beginTransaction()
                results.deleteAllFromRealm()
                mRealm.commitTransaction()

                val resultIntent = Intent(applicationContext, TaskAlarmReceiver::class.java)
                val resultPendingIntent = PendingIntent.getBroadcast(
                    this@MainActivity,
                    task.id,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )

                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(resultPendingIntent)
                Log.d("TaskApp", "Delete alarm.")

                reloadListView()
            }

            builder.setNegativeButton("CANCEL", null)

            val dialog = builder.create()
            dialog.show()

            true
        }

        reloadListView()
    }

    override fun onDestroy() {
        super.onDestroy()

        mRealm.close()
    }

    private fun reloadListView(withSearchTerm: String? = null) {

        var taskRealmResults: RealmResults<Task>?

        // category検索欄に文字列の入力があり、withSearchTermがnullでない
        if (withSearchTerm != null) {

            // Realmデータベースから、「withSearchTermを含むcategory全てのデータを取得して新しい日時順に並べた結果」を取得
//            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
            taskRealmResults = mRealm.where(Task::class.java).contains("category", withSearchTerm!!).findAll().sort("date", Sort.DESCENDING)

        } else {

            // Realmデータベースから、「全てのデータを取得して新しい日時順に並べた結果」を取得
            taskRealmResults = mRealm.where(Task::class.java).findAll().sort("date", Sort.DESCENDING)
        }


        // 上記の結果を、TaskList としてセットする
        mTaskAdapter.taskList = mRealm.copyFromRealm(taskRealmResults!!)


        // TaskのListView用のアダプタに渡す
        listView1.adapter = mTaskAdapter

        // 表示を更新するために、アダプターにデータが変更されたことを知らせる
        mTaskAdapter.notifyDataSetChanged()

    }

}//end
