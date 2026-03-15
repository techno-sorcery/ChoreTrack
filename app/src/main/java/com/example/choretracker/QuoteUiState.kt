import com.example.choretracker.Quote

sealed class QuoteUiState {

    data class Success(
        val quote: Quote
    ) : QuoteUiState()

    data class Error(
        val message: String
    ) : QuoteUiState()

    data object Loading : QuoteUiState()
}