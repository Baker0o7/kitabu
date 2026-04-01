package com.kitabu.app.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.kitabu.app.R
import com.kitabu.app.ui.notes.MainActivity

class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREF_NAME = "kitabu_prefs"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

        fun isOnboardingCompleted(context: Context): Boolean {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_COMPLETED, false)
        }

        fun setOnboardingCompleted(context: Context) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        }
    }

    private val pages = listOf(
        OnboardingPage(
            emoji = "📓",
            title = "Welcome to Kitabu",
            description = "Your Second Brain — a powerful note-taking app designed to help you capture, connect, and organize your thoughts effortlessly."
        ),
        OnboardingPage(
            emoji = "✨",
            title = "Powerful Features",
            description = "• Markdown editing with live preview\n• Wikilinks to connect your notes\n• AI-powered writing assistant\n• Beautiful themes to match your style\n• End-to-end encryption for private notes"
        ),
        OnboardingPage(
            emoji = "🚀",
            title = "Get Started",
            description = "Start capturing your ideas. Create your first note and discover how Kitabu becomes your trusted knowledge companion."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
        val dotsContainer = findViewById<LinearLayout>(R.id.dotsContainer)
        val btnSkip = findViewById<MaterialButton>(R.id.btnSkip)
        val btnNext = findViewById<MaterialButton>(R.id.btnNext)

        viewPager.adapter = OnboardingAdapter(pages)
        setupDots(dotsContainer, pages.size)

        viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(dotsContainer, position)
                if (position == pages.size - 1) {
                    btnNext.text = "Let's Go!"
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.text = "Next"
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.size - 1) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupDots(container: LinearLayout, count: Int) {
        container.removeAllViews()
        for (i in 0 until count) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 8.dpToPx()).apply {
                    marginEnd = 8.dpToPx()
                }
                setBackgroundResource(R.drawable.bg_preview_dot)
            }
            container.addView(dot)
        }
        updateDots(container, 0)
    }

    private fun updateDots(container: LinearLayout, activeIndex: Int) {
        for (i in 0 until container.childCount) {
            val dot = container.getChildAt(i)
            val params = dot.layoutParams as LinearLayout.LayoutParams
            if (i == activeIndex) {
                params.width = 32.dpToPx()
                dot.setBackgroundColor(getColor(R.color.accent))
            } else {
                params.width = 24.dpToPx()
                dot.setBackgroundColor(getColor(R.color.text_hint))
            }
            dot.layoutParams = params
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun finishOnboarding() {
        setOnboardingCompleted(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private data class OnboardingPage(
        val emoji: String,
        val title: String,
        val description: String
    )

    private class OnboardingAdapter(
        private val pages: List<OnboardingPage>
    ) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvEmoji: TextView = view.findViewById(R.id.tvEmoji)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_onboarding_page, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = pages[position]
            holder.tvEmoji.text = page.emoji
            holder.tvTitle.text = page.title
            holder.tvDescription.text = page.description
        }

        override fun getItemCount() = pages.size
    }
}
