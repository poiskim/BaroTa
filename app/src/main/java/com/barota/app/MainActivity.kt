package com.barota.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.barota.app.domain.model.ArrivalStatus
import com.barota.app.presentation.home.HomeScreen
import com.barota.app.presentation.home.HomeViewModel
import com.barota.app.presentation.search.SearchScreen
import com.barota.app.presentation.search.SearchViewModel
import com.barota.app.presentation.theme.BaroTaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BaroTaApp
        setContent {
            BaroTaTheme {
                val homeVm: HomeViewModel = viewModel(factory = HomeViewModel.factory(app.container))
                val searchVm: SearchViewModel = viewModel(factory = SearchViewModel.factory(app.container))
                BaroTaAppScreen(homeVm, searchVm)
            }
        }
    }
}

@Composable
private fun BaroTaAppScreen(viewModel: HomeViewModel, searchVm: SearchViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val searchState by searchVm.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> if (grants.values.any { it }) viewModel.refreshLocation() }
    val requestLocation = {
        if (hasPermission()) viewModel.refreshLocation()
        else permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Text("⌂") }, label = { Text("홈") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { searchVm.reset(); tab = 1 },
                    icon = { Text("⌕") }, label = { Text("검색·추가") }
                )
            }
        }
    ) { padding ->
        if (tab == 0) {
            HomeScreen(
                state = state,
                onLocate = requestLocation,
                onSearch = { selected ->
                    if (selected == null) searchVm.reset() else searchVm.searchRoutes(selected)
                    tab = 1
                },
                onOpenDetail = { viewModel.openDetail(it.favorite) },
                onPin = viewModel::togglePinned,
                onDelete = viewModel::remove,
                modifier = Modifier.padding(padding)
            )
        } else {
            SearchScreen(
                state = searchState,
                currentLocation = state.location,
                onUpdateQuery = searchVm::updateQuery,
                onSearchRoutes = searchVm::searchRoutes,
                onSelectRoute = searchVm::selectRoute,
                onUpdateStopFilter = searchVm::updateStopFilter,
                onSelectStop = searchVm::selectStop,
                onSaveSelectedStop = searchVm::saveVerifiedBusStop,
                onSaveSubway = searchVm::saveSubwayManual,
                modifier = Modifier.padding(padding)
            )
        }
    }

    detail?.let { selected ->
        val message = when (val arrival = selected.arrival) {
            ArrivalStatus.NotConnected -> "실시간 도착정보는 아직 연결되지 않았어요."
            is ArrivalStatus.Available -> "${arrival.minutes}분 후 도착" +
                (arrival.nextMinutes?.let { " · 다음 차 ${it}분" } ?: "")
            is ArrivalStatus.Failure -> arrival.message
        }
        AlertDialog(
            onDismissRequest = viewModel::closeDetail,
            title = { Text("${selected.item.route} · ${selected.item.stop}") },
            text = { Text("${selected.item.direction}\n\n" +
                (if (selected.item.isOfficiallyVerified) "공식 GBIS 노선·정류장 검증됨" else "미검증·기존 수동 입력") +
                "\n\n$message\n\n조회 횟수는 추천에 반영됩니다.") },
            confirmButton = { TextButton(onClick = viewModel::closeDetail) { Text("닫기") } }
        )
    }
}
