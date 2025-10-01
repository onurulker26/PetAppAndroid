package com.tamer.petapp.guide

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.tamer.petapp.R
import com.tamer.petapp.databinding.ItemGuideContentBinding
import com.tamer.petapp.model.PetCareGuide

class PetCareGuideContentAdapter(
    private val guide: PetCareGuide
) : RecyclerView.Adapter<PetCareGuideContentAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGuideContentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(guide.sections[position])
    }

    override fun getItemCount() = guide.sections.size

    inner class ViewHolder(
        private val binding: ItemGuideContentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: PetCareGuide.Section) {
            // Bölüm başlığı
            binding.tvTitle.text = section.title
            
            // İçerik metnini biçimlendir
            val contentText = StringBuilder()
            section.content.forEachIndexed { index, item ->
                contentText.append(item)
                if (index < section.content.size - 1) {
                    contentText.append("\n\n")
                }
            }
            
            // Her madde işaretini vurgula
            val content = contentText.toString()
            val spannable = SpannableString(content)
            
            // Bullet işaretlerini (•) renklendirme
            val context = binding.root.context
            val accentColor = ContextCompat.getColor(context, R.color.accent)
            val colorPrimary = ContextCompat.getColor(context, R.color.primary)
            
            var startIndex = content.indexOf("•")
            while (startIndex >= 0) {
                spannable.setSpan(
                    ForegroundColorSpan(accentColor),
                    startIndex,
                    startIndex + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                // Madde başlığını kalın yapma
                val lineEnd = content.indexOf("\n", startIndex)
                val endIndex = if (lineEnd > 0) lineEnd else content.length
                
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    startIndex + 1,
                    endIndex,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                
                startIndex = content.indexOf("•", startIndex + 1)
            }
            
            binding.tvContent.text = spannable
        }
    }
} 