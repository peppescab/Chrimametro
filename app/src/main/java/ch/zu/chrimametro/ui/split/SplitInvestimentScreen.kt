package ch.zu.chrimametro.ui.split

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SplitInvestimentScreen(viewModel: SplitViewModel) {

    val splitState by viewModel.myStateFlow.collectAsState()
    Column {
        splitState.forEach {
            SplitItemCard(it)
        }
    }
}
