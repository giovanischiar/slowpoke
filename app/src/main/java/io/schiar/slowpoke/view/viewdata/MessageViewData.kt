package io.schiar.slowpoke.view.viewdata

import android.os.Parcel
import android.os.Parcelable
import java.util.Date
import java.util.Calendar

data class MessageViewData(
    val origin: Boolean,
    val content: String,
    val date: Date = Calendar.getInstance().time
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readByte() != 0.toByte(),
        parcel.readString() ?: "",
        Date(parcel.readLong())
    )
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel?, flags: Int) {
        out?.writeByte(if (origin) 0 else 1)
        out?.writeString(content)
        out?.writeLong(date.time)
    }

    companion object CREATOR : Parcelable.Creator<MessageViewData> {
        override fun createFromParcel(parcel: Parcel): MessageViewData {
            return MessageViewData(parcel)
        }

        override fun newArray(size: Int): Array<MessageViewData?> {
            return arrayOfNulls(size)
        }
    }
}