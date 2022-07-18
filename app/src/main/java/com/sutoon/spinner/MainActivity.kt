package com.sutoon.spinner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sutoon.spinner.views.NiceSpinnerPro
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val spinner: NiceSpinnerPro = findViewById(R.id.niceSpinnerPro)
        val dataset: List<String> = LinkedList(Arrays.asList("One", "Two", "Three"))
//        val dataset1: List<Int> = LinkedList(
//            listOf(
//                R.drawable.ic_icon_photo_style1,
//                R.drawable.ic_icon_photo_style2,
//                R.drawable.ic_icon_photo_style3,
//            )
//        )
        val dataset2:List<Int> = LinkedList(
            listOf(
                R.color.black,
                R.color.purple_500,
                R.color.purple_200
            )

        )

        spinner.attachDataSource(dataset, dataset2, false, true)
    }
}