package com.cloudlevi.ping.ui.home

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.cloudlevi.ping.ui.home.HomeFragmentEvent.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudlevi.ping.HOMEFRAGMENT_GRIDVIEW
import com.cloudlevi.ping.HOMEFRAGMENT_LISTVIEW
import com.cloudlevi.ping.R
import com.cloudlevi.ping.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class HomeFragment: Fragment(R.layout.fragment_home), PostsAdapter.OnPostClickedListener {

    private val viewModel: HomeFragmentViewModel by activityViewModels()
    private lateinit var binding: FragmentHomeBinding
    private lateinit var postsAdapter: PostsAdapter
    private val listener = this
    private var boolSearch = false

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentHomeBinding.bind(view)
        postsAdapter = PostsAdapter(viewModel.listType, listener)

        updateViewButtonSrc()
        if (arguments != null)
        {
            boolSearch = HomeFragmentArgs.fromBundle(requireArguments()).boolSearch
            if (boolSearch)
                viewModel.applySearchWithFilters()
        }

        binding.apply {

            apartmentsRecyclerView.apply {
                adapter = postsAdapter
                layoutManager = getFragmentLayoutManager()
                setHasFixedSize(true)
            }

            viewModel.homeLiveData.observe(viewLifecycleOwner){
                postsAdapter.submitList(it)
                postsAdapter.notifyDataSetChanged()

                if (it.isNullOrEmpty()) nothingFoundTV.visibility = View.VISIBLE
                else nothingFoundTV.visibility = View.GONE
            }

            changeLayoutManager.setOnClickListener {
                viewModel.listTypeChanged()

                postsAdapter = PostsAdapter(viewModel.listType, listener)
                updateViewButtonSrc()
                postsAdapter.submitList(viewModel.homeLiveData.value)

                apartmentsRecyclerView.apply {
                    adapter = postsAdapter
                    layoutManager = getFragmentLayoutManager()
                    setHasFixedSize(true)
                }
            }

            searchEditText.setText(viewModel.searchText)

            searchEditText.addTextChangedListener {
                if (it != null)
                    viewModel.searchText = it.toString().trim()
                else viewModel.searchText = ""
            }

            searchEditText.setOnEditorActionListener { textView, i, keyEvent ->
                if (i == EditorInfo.IME_ACTION_GO)
                {
                    viewModel.applySearchWithFilters()
                    return@setOnEditorActionListener true
                }
                false
            }

            filterButton.setOnClickListener {
                viewModel.onFilterClicked()
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToFiltersFragment())
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.homeFragmentEvent.collect { event ->
                    when(event){
                        is SendToastMessage -> sendToastMessage(event.message)
                        is ChangeProgressStatus -> changeProgressStatus(event.status)
                    }
                }
            }

            if (viewModel.apartmentsList.isEmpty() && !boolSearch)
                viewModel.observeApartmentsList()
        }
    }

    private fun updateViewButtonSrc(){
        when(viewModel.listType){
            HOMEFRAGMENT_LISTVIEW -> binding.changeLayoutManager.setImageResource(R.drawable.ic_listview)
            HOMEFRAGMENT_GRIDVIEW -> binding.changeLayoutManager.setImageResource(R.drawable.ic_gridview)
        }
    }

    private fun getFragmentLayoutManager(): RecyclerView.LayoutManager{
        return when(viewModel.listType){
            HOMEFRAGMENT_LISTVIEW -> LinearLayoutManager(requireContext())
            HOMEFRAGMENT_GRIDVIEW -> GridLayoutManager(requireContext(), 2, GridLayoutManager.VERTICAL, false)
            else -> LinearLayoutManager(requireContext())
        }
    }



    private fun changeProgressStatus(status: Int){
        binding.progressBar.visibility = status

        when(status){
            View.GONE -> binding.mainRelativeLayout.foreground = null
            View.VISIBLE -> binding.mainRelativeLayout.foreground = ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
        }
    }

    private fun sendToastMessage(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

    override fun OnItemClickedListener(apartmentID: String) {
        val action = HomeFragmentDirections.actionHomeFragmentToApartmentPageFragment(apartmentID)
        findNavController().navigate(action)
    }
}