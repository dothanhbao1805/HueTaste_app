package com.base.common.base

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class LsDialog<V : ViewBinding>(val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> V) :
    DialogFragment() {

    lateinit var binding: V

    abstract fun isFull(): Boolean


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        this.dialog?.window?.setBackgroundDrawableResource(R.color.backgroundDialogOrange)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewCreated()

    }

    abstract fun onViewCreated()
    override fun onStart() {
        super.onStart()
        dialog?.apply {
            window?.apply {
                setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    if (isFull()) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT)
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }
}