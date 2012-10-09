package eu.tsvetkov.rabota.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import eu.tsvetkov.rabota.R;
import eu.tsvetkov.rabota.provider.TaskContract;

public class TaskListActivity extends FragmentActivity {

	private static final String TAG = TaskListActivity.class.getSimpleName();

	private ListFragment mTaskListFragment;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		if (intent.getData() == null) {
			intent.setData(TaskContract.Tasks.CONTENT_URI);
		}

		setContentView(R.layout.task_list);

		mTaskListFragment = (ListFragment) getSupportFragmentManager().findFragmentById(android.R.id.list);
	}

	private void animateRotation(ImageView iconProgress) {
		Animation rotation = AnimationUtils.loadAnimation(TaskListActivity.this, R.anim.rotate_indefenitely);
		rotation.setRepeatCount(Animation.INFINITE);
		iconProgress.startAnimation(rotation);
	}

}
