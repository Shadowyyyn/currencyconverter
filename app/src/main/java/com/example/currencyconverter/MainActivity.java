package com.example.currencyconverter;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private EditText etAmount;
    private Spinner spinnerFrom, spinnerTo;
    private TextView tvResult;

    private List<String> currencies = new ArrayList<>();
    private ArrayAdapter<String> adapter;  // Явно указываем тип
    private CurrencyApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        setupApiService();
        fetchCurrencies();
    }

    private void initViews() {
        etAmount = findViewById(R.id.etAmount);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        Button btnConvert = findViewById(R.id.btnConvert);
        tvResult = findViewById(R.id.tvResult);

        btnConvert.setOnClickListener(v -> convertCurrency());
    }

    private void setupSpinners() {
        // Создаем адаптер с явно указанным типом
        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                currencies
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Добавляем слушатели для отладки (опционально)
        spinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Ничего не делаем при выборе
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не делаем
            }
        });
    }

    private void setupApiService() {
        apiService = ApiClient.getClient().create(CurrencyApiService.class);
    }

    private void fetchCurrencies() {
        Toast.makeText(this, "Загрузка валют...", Toast.LENGTH_SHORT).show();

        Call<CurrencyResponse> call = apiService.getLatestRates("RUB");
        call.enqueue(new Callback<CurrencyResponse>() {
            @Override
            public void onResponse(Call<CurrencyResponse> call, Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateCurrenciesList(response.body());
                } else {
                    Toast.makeText(MainActivity.this,
                            "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                    loadDefaultCurrencies();
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                loadDefaultCurrencies();
            }
        });
    }

    private void updateCurrenciesList(CurrencyResponse response) {
        currencies.clear();

        // Добавляем базовую валюту
        currencies.add(response.getBase());

        // Добавляем все валюты из rates
        if (response.getRates() != null) {
            currencies.addAll(response.getRates().keySet());
        }

        // Обновляем адаптер
        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();

            // Устанавливаем начальные значения (опционально)
            if (currencies.size() > 0) {
                spinnerFrom.setSelection(0);
                if (currencies.size() > 1) {
                    spinnerTo.setSelection(1);
                }
            }
        });
    }

    private void loadDefaultCurrencies() {
        currencies.clear();
        currencies.add("USD");
        currencies.add("EUR");
        currencies.add("RUB");
        currencies.add("GBP");
        currencies.add("JPY");
        currencies.add("CNY");

        runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
        });
    }

    private void convertCurrency() {
        String amountStr = etAmount.getText().toString().trim();

        if (amountStr.isEmpty()) {
            etAmount.setError("Введите сумму");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            etAmount.setError("Некорректная сумма");
            return;
        }

        // Проверяем, что валюты выбраны
        if (spinnerFrom.getSelectedItem() == null || spinnerTo.getSelectedItem() == null) {
            Toast.makeText(this, "Выберите валюты", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromCurrency = spinnerFrom.getSelectedItem().toString();
        String toCurrency = spinnerTo.getSelectedItem().toString();

        if (fromCurrency.equals(toCurrency)) {
            tvResult.setText(String.format("%.2f %s", amount, toCurrency));
            return;
        }

        fetchConversionRate(fromCurrency, toCurrency, amount);
    }

    private void fetchConversionRate(String from, String to, double amount) {
        Call<CurrencyResponse> call = apiService.getLatestRates(from);

        call.enqueue(new Callback<CurrencyResponse>() {
            @Override
            public void onResponse(Call<CurrencyResponse> call, Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Double rate = response.body().getRates().get(to);
                    if (rate != null) {
                        double result = amount * rate;
                        String formattedResult = String.format("%.2f %s", result, to);
                        tvResult.setText(formattedResult);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "Курс для данной валюты не найден", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            "Ошибка получения курса", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}