package com.hover.stax.transactions;

import android.content.Context
import com.hover.sdk.actions.HoverAction
import com.hover.sdk.transactions.Transaction
import com.hover.stax.R

class TransactionStatus(val transaction: StaxTransaction) {

    fun getIcon(action: HoverAction?, c: Context): Int {
        return when (transaction.status) {
            Transaction.FAILED -> R.drawable.ic_info_red
            Transaction.PENDING -> {
                if (transaction.isRecorded) R.drawable.ic_warning
                else R.drawable.ic_info
            }
            else -> 0
        }
    }

    fun getBackgroundColor(): Int {
        return when (transaction.status) {
            Transaction.FAILED -> R.color.cardDarkRed
            Transaction.PENDING -> if (transaction.isRecorded) R.color.pending_brown else R.color.cardDarkBlue
            else -> R.color.muted_green
        }
    }

    fun getTitle(): Int {
        return when (transaction.status) {
            Transaction.FAILED -> R.string.failed_cardHead
            Transaction.PENDING -> if (transaction.isRecorded) R.string.checking_your_flow else R.string.pending_cardHead
            else -> R.string.succeeded_cardHead
        }
    }

    fun getShortStatusDetail(action: HoverAction?, c: Context): String {
        if (transaction.isRecorded) return getRecordedStatusDetail(c)
        else return when (transaction.status) {
            Transaction.FAILED -> lookupFailureMessage(action, c)
            Transaction.PENDING -> c.getString(R.string.pending_cardhead)
            else -> {
                return if (transaction.balance == null) "\\u2014"
                else transaction.balance
            }
        }
    }

    fun getStatusDetail(action: HoverAction, c: Context): String {
        if (transaction.isRecorded) return getRecordedStatusDetail(c)
        else return when (transaction.status) {
            Transaction.FAILED -> lookupFailureDescription(action, c)
            Transaction.PENDING -> c.getString(R.string.pending_cardbody)
            else -> {
                return if (transaction.balance == null) "\\u2014"
                else c.getString(R.string.new_balance, transaction.balance)
            }
        }
    }

    private fun getRecordedStatusDetail(c: Context): String {
        return c.getString(when (transaction.status) {
            Transaction.FAILED -> R.string.bounty_transaction_failed
            Transaction.PENDING -> R.string.bounty_flow_pending_dialog_msg
            else -> R.string.flow_done_desc
        })
    }

    private fun lookupFailureMessage(a: HoverAction?, c: Context): String {
        return when (transaction.category) {
            StaxTransaction.BALANCE_ERROR -> c.getString(R.string.balance_error, getServiceName(a, c))
            StaxTransaction.PIN_ERROR -> c.getString(R.string.pin_error, getServiceName(a, c))
            StaxTransaction.INVALID_ENTRY_ERROR -> c.getString(R.string.invalid_entry)
            StaxTransaction.MMI_ERROR -> c.getString(R.string.mmi_error)
            StaxTransaction.UNREGISTERED_ERROR -> c.getString(R.string.unregistered_error, getServiceName(a, c))
            StaxTransaction.INCOMPLETE_ERROR -> c.getString(R.string.incomplete_error, getServiceName(a, c))
            StaxTransaction.NO_RESPONSE_ERROR -> c.getString(R.string.no_response_error, getServiceName(a, c))
            else -> c.getString(R.string.unspecified_error)
        }
    }

    private fun lookupFailureDescription(a: HoverAction?, c: Context): String {
        return when (transaction.category) {
            StaxTransaction.BALANCE_ERROR -> c.getString(R.string.balance_error, getServiceName(a, c))
            StaxTransaction.PIN_ERROR -> c.getString(R.string.pin_error, getServiceName(a, c))
            StaxTransaction.INVALID_ENTRY_ERROR -> c.getString(R.string.invalid_entry_desc)
            StaxTransaction.MMI_ERROR -> c.getString(R.string.mmi_error_desc)
            StaxTransaction.UNREGISTERED_ERROR -> c.getString(R.string.unregistered_error, getServiceName(a, c))
            StaxTransaction.INCOMPLETE_ERROR -> c.getString(R.string.incomplete_desc, getServiceName(a, c))
            StaxTransaction.NO_RESPONSE_ERROR -> c.getString(R.string.no_response_desc, getServiceName(a, c))
            else -> c.getString(R.string.unspecified_error_desc)
        }
    }

    private fun getServiceName(a: HoverAction?, c: Context): String {
        if (a == null) return c.getString(R.string.null_service_name_text) else return a.from_institution_name
    }
}
