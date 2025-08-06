package github.buriedincode

data class ErrorResponse(val title: String, val status: Int, val type: String, val details: Map<String, String>?)
