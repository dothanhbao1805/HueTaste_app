package com.base.common.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewbinding.ViewBinding

abstract class LsFragmentNew<VB : ViewBinding>(val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB) : Fragment() {

    lateinit var binding: VB

    abstract fun onViewCreated()

    abstract fun initObservable(event: Lifecycle.Event = Lifecycle.Event.ON_DESTROY)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onViewCreated()
        initObservable()
    }

}
