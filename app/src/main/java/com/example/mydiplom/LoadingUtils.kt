import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.ImageView
import com.example.mydiplom.R

object LoadingUtils {

    private var loadingDialog: Dialog? = null

    @SuppressLint("MissingInflatedId")
    fun showLoading(context: Context) {
        if (loadingDialog == null) {
            loadingDialog = Dialog(context).apply {
                setContentView(R.layout.dialog_loading)
                setCancelable(false)
                window?.setBackgroundDrawableResource(android.R.color.transparent)

            }
        }
        loadingDialog?.show()
    }

    fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
}

