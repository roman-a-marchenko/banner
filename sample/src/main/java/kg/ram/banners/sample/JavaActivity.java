package kg.ram.banners.sample;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import org.imaginativeworld.banners.sample.databinding.ActivityJavaBinding;

public class JavaActivity extends AppCompatActivity {

    private ActivityJavaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJavaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}
