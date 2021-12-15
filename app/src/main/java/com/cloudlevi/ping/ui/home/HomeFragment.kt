package com.cloudlevi.ping.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.activityViewModels
import com.cloudlevi.ping.ui.home.HomeFragmentEvent.*
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cloudlevi.ping.*
import com.cloudlevi.ping.data.SortOrder
import com.cloudlevi.ping.databinding.FragmentHomeBinding
import com.cloudlevi.ping.ext.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import com.cloudlevi.ping.ui.home.HomeFragmentViewModel.Action
import com.cloudlevi.ping.ui.home.HomeFragmentViewModel.ActionType.*
import java.lang.RuntimeException

@AndroidEntryPoint
class HomeFragment :
    BaseFragment<FragmentHomeBinding>
        (R.layout.fragment_home, true), PostsAdapter.OnPostClickedListener {

    private val viewModel: HomeFragmentViewModel by activityViewModels()
    private lateinit var binding: FragmentHomeBinding
    private lateinit var postsAdapter: PostsAdapter
    private val listener = this
    private var boolSearch = false

    override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentHomeBinding =
        FragmentHomeBinding::inflate

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        viewModel.action.observe(viewLifecycleOwner) {
            val data = it.getDataSafely() ?: return@observe
            doAction(data)
        }

        if (arguments != null) {
            boolSearch = HomeFragmentArgs.fromBundle(requireArguments()).boolSearch
            if (boolSearch)
                viewModel.updateAndFilterCurrentList()
        }

        viewModel.fragmentCreated(boolSearch)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postsAdapter = PostsAdapter(viewModel.listType, listener)

        updateViewButtonSrc()

        binding.apply {

            apartmentsRecyclerView.apply {
                adapter = postsAdapter
                layoutManager = getFragmentLayoutManager()
                setHasFixedSize(true)
            }

            changeLayoutManager.setOnClickListener {
                viewModel.listTypeChanged()

                postsAdapter = PostsAdapter(viewModel.listType, listener)
                updateViewButtonSrc()
                postsAdapter.submitList(viewModel.displayedApartments)

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
                if (i == EditorInfo.IME_ACTION_GO) {
                    viewModel.updateAndFilterCurrentList()
                    return@setOnEditorActionListener true
                }
                false
            }

            filterButton.setOnClickListener {
                findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToFiltersFragment())
            }

            viewLifecycleOwner.lifecycleScope.launchWhenStarted {
                viewModel.homeFragmentEvent.collect { event ->
                    when (event) {
                        is SendToastMessage -> sendToastMessage(event.message)
                        is ChangeProgressStatus -> changeProgressStatus(event.status)
                    }
                }
            }

            if (viewModel.allApartments.isEmpty() && !boolSearch)
                viewModel.observeApartmentsList()

            sortTV.text = getStringForEnum(requireContext(), viewModel.sortBy)
            changeSortingDirection(viewModel.sortOrder)
            updateSearchSection()

            sortBySection.setOnClickListener {
                showSortingDialog(
                    requireContext(),
                    viewModel.sortBy,
                    viewModel.sortOrder
                ) { sortEnum, sortOrder ->
                    sortTV.text = getStringForEnum(requireContext(), sortEnum)
                    changeSortingDirection(sortOrder)
                    viewModel.updateSortOrder(sortEnum, sortOrder)
                }
            }
        }
    }

    private fun changeSortingDirection(sortOrder: SortOrder) {
        binding.apply {
            if (sortOrder == SortOrder.NONE) {
                sortDirection.makeGone()
                return
            }
            val targetRotation = if (sortOrder == SortOrder.DESCENDING) 0f else 180f

            sortDirection.apply {
                if (!isVisible) {
                    rotation = targetRotation
                    makeVisible()
                } else {
                    animate().cancel()
                    animate().rotation(targetRotation).setDuration(150).start()
                }
            }
        }
    }

    private fun updateSearchSection() {
        binding.apply {
            if (viewModel.searchText.isNotEmpty()) {
                resultsForTV.text = getString(R.string.showing_results_for_, viewModel.searchText)
                resultsForTV.makeVisible()
            } else resultsForTV.makeGone()
        }
    }

    private fun updateViewButtonSrc() {
        when (viewModel.listType) {
            HOMEFRAGMENT_LISTVIEW -> binding.changeLayoutManager.setImageResource(R.drawable.ic_listview)
            HOMEFRAGMENT_GRIDVIEW -> binding.changeLayoutManager.setImageResource(R.drawable.ic_gridview)
        }
    }

    private fun doAction(action: Action) {
        when (action.type) {
            LIST_UPDATED -> listUpdated(action.bool ?: false)
        }
    }

    private fun listUpdated(overrideNothingFound: Boolean) {
        Log.d("TAG", "listUpdated: ${viewModel.displayedApartments}")
        binding.apply {
            updateSearchSection()
            postsAdapter.submitList(viewModel.displayedApartments)
            //postsAdapter.notifyDataSetChanged()

            changeProgressStatus(View.GONE)

            if (overrideNothingFound) nothingFoundTV.makeGone()
            else nothingFoundTV.visibleOrGone(viewModel.displayedApartments.isNullOrEmpty())
        }
    }

    private fun getFragmentLayoutManager(): RecyclerView.LayoutManager {
        return when (viewModel.listType) {
            HOMEFRAGMENT_LISTVIEW -> LinearLayoutManager(requireContext())
            HOMEFRAGMENT_GRIDVIEW -> GridLayoutManager(
                requireContext(),
                2,
                GridLayoutManager.VERTICAL,
                false
            )
            else -> LinearLayoutManager(requireContext())
        }
    }


    private fun changeProgressStatus(status: Int) {
//        binding.progressBar.visibility = status
//
//        when (status) {
//            View.GONE -> binding.mainRelativeLayout.foreground = null
//            View.VISIBLE -> binding.mainRelativeLayout.foreground =
//                ContextCompat.getDrawable(requireContext(), R.color.black_transparent)
//        }

        val isLoading = status == View.VISIBLE
        (requireActivity() as MainActivity).switchLoading(isLoading)
        toggleAllViewsEnabled(!isLoading, binding.root)
    }

    private fun sendToastMessage(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

    override fun oldOnItemClickedListener(apartmentID: String) {
    }

    override fun onItemClickedListener(pos: Int) {
        val action = HomeFragmentDirections
            .actionHomeFragmentToApartmentPageFragment(viewModel.displayedApartments[pos])
        findNavController().navigate(action)
    }
}