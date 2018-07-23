/*
 * Copyright 2018 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.plaidapp.designernews.ui.login

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.net.Uri
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.transition.TransitionManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import io.plaidapp.core.ui.transitions.FabTransform
import io.plaidapp.core.ui.transitions.MorphTransform
import io.plaidapp.core.util.delegates.contentView
import io.plaidapp.core.util.doAfterTextChanged
import io.plaidapp.core.util.event.EventObserver
import io.plaidapp.designernews.BR
import io.plaidapp.designernews.R
import io.plaidapp.designernews.databinding.ActivityDesignerNewsLoginBinding
import io.plaidapp.designernews.provideViewModelFactory
import io.plaidapp.R as appR

class LoginActivity : AppCompatActivity() {

    private val binding by contentView<LoginActivity, ActivityDesignerNewsLoginBinding>(
        R.layout.activity_designer_news_login
    )

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val factory = provideViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(LoginViewModel::class.java)

        binding.viewModel = viewModel

        viewModel.uiState.observe(this, Observer<LoginUiModel?> { uiModel ->
            if (uiModel?.showProgress == true) {
                beginDelayedTransition()
            }

            if (uiModel?.showError != null && !uiModel.showError.consumed) {
                showLoginFailed(uiModel.showError.peek())
            }
            if (uiModel?.showSuccess != null && !uiModel.showSuccess.consumed) {
                val userData = uiModel.showSuccess.peek()
                updateUiWithUser(userData)
                setResult(Activity.RESULT_OK)
                finish()
            }

            binding.loginUiModel = uiModel
        })

        viewModel.openUrl.observe(this, EventObserver { openUrl(it) })

        if (!FabTransform.setup(this, binding.container)) {
            MorphTransform.setup(
                this,
                binding.container,
                ContextCompat.getColor(this, appR.color.background_light),
                resources.getDimensionPixelSize(appR.dimen.dialog_corners)
            )
        }

        binding.username.doAfterTextChanged {
            viewModel.loginDataChanged(it.toString(), binding.password.text.toString())
        }
        binding.password.apply {
            doAfterTextChanged {
                viewModel.loginDataChanged(binding.username.text.toString(), it.toString())
            }
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    viewModel.login(
                        binding.username.text.toString(),
                        binding.password.text.toString()
                    )
                }
                false
            }
        }
    }

    override fun onBackPressed() {
        dismiss(null)
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    fun dismiss(view: View?) {
        setResult(Activity.RESULT_CANCELED)
        finishAfterTransition()
    }

    fun login(view: View?) {
        viewModel.login(binding.username.text.toString(), binding.password.text.toString())
    }

    private fun updateUiWithUser(uiModel: SuccessLoginUiModel) {
        val binding: ViewDataBinding = DataBindingUtil.inflate<ViewDataBinding>(
            LayoutInflater.from(this@LoginActivity),
            R.layout.toast_logged_in_confirmation, null, false
        )
        binding.setVariable(BR.uiModel, uiModel)

        // need to use app context here as the activity will be destroyed shortly
        Toast(applicationContext).apply {
            view = binding.root
            setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
            duration = Toast.LENGTH_LONG
        }.show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        binding.container.let {
            Snackbar.make(it, errorString, Snackbar.LENGTH_SHORT).show()
        }
        beginDelayedTransition()
        binding.password.requestFocus()
    }

    private fun beginDelayedTransition() {
        TransitionManager.beginDelayedTransition(binding.container)
    }
}
