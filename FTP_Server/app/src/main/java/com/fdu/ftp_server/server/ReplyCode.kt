package server



/**
 * Reply codes of the FTP protocol
 *
 * rfc959 P37~43
 */

/*
 * Positive initiation
 */
const val RESTART_MARKER_REPLY = 110
const val STARTING_TRANSFER = 125
const val OPENING_BINARY = 150

/*
 * Positive termination
 */
const val COMMAND_OK = 200
const val HELP = 211
const val LAST_MODIFIED = 213
const val SYSTEM = 215
const val SERVICE_RDY = 220
const val DISCONNECTION = 221
const val CLOSING_DATA_CONNECTION = 226
const val ENTERING_PASSIVE = 227
const val ENTERING_EXTENDED_PASSIVE = 229
const val USER_LOGGED_IN = 230
const val ACT_FILE_OK = 250
const val PATH = 257

/*
 * Positive, continue
 */
const val USER_OK = 331
const val FILE_OK_CONTINUE = 350

/*
 * Negative
 */
const val TIMEOUT = 421
const val CONNECTION_CLOSED = 426

/*
 * Negative termination
 */
const val SYNTAX_ERROR = 500
const val ERROR_ARGS = 501
const val COMMAND_NOT_IMPLEMENTED = 502
const val BAD_SEQUENCE_COMMANDS = 503
const val COMMAND_NOT_IMPLEMENTED_FOR_PARAM = 504
const val NOT_CONNECTED = 530
const val FILE_NOT_OK = 550
