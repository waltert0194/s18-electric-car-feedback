package fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import asc.clemson.electricfeedback.R;


public class StartFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_start, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button manBtn = getView().findViewById(R.id.button1);
        Button trackBtn = getView().findViewById(R.id.button2);

        manBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment fragment = new MapsFragment();
                replaceFragment(fragment);
            }
        });
    }

    public void replaceFragment(Fragment someFragment) {
        android.app.FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.start_frame, someFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}
