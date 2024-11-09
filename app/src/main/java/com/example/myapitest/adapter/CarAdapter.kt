import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapitest.R
import com.example.myapitest.model.CarDetails

class CarAdapter(
    private val cars: List<CarDetails>,
    private val itemClickListener: (CarDetails) -> Unit,
) : RecyclerView.Adapter<CarAdapter.ItemViewHolder>() {

    class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image)
        val tvModel: TextView = view.findViewById(R.id.model)
        val tvYear: TextView = view.findViewById(R.id.year)
        val tvLicensePlate: TextView = view.findViewById(R.id.license)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_layout, parent, false)
        return ItemViewHolder(view)
    }

    override fun getItemCount(): Int = cars.size

    override fun onBindViewHolder(holder: ItemViewHolder, index: Int) {
        val car = cars[index]
        holder.itemView.setOnClickListener {
            itemClickListener.invoke(car)
        }
        holder.tvModel.text = car.name

        holder.tvYear.text = car.year

        holder.tvLicensePlate.text = car.licence

        //TODO: holder.imageView.loadUrl(item.imageUrl)
    }
}