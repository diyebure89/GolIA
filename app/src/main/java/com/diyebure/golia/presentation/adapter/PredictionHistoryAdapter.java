package com.diyebure.golia.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.diyebure.golia.R;
import com.diyebure.golia.presentation.ui.history.PredictionHistoryUiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the prediction history list ({@code HistorialPronosticosActivity}, R8).
 *
 * <p>Binds a list of {@link PredictionHistoryUiModel}: each row shows the team
 * names (or the "Partido no disponible" placeholder when the match is missing,
 * R8.3), the predicted score, a status label (pendiente / acertado / fallido,
 * R8.4) coloured accordingly, and the earned points only when the prediction is
 * resolved (R8.4, R8.5).</p>
 *
 * <p>Expected view IDs in {@code item_prediction_history.xml}:</p>
 * <ul>
 *     <li>{@code text_match_label} — team names or placeholder.</li>
 *     <li>{@code text_predicted_score} — predicted score "2 - 1".</li>
 *     <li>{@code text_status} — status label, tinted per {@link PredictionHistoryUiModel.Status}.</li>
 *     <li>{@code text_points} — earned points; hidden while pending.</li>
 * </ul>
 */
public class PredictionHistoryAdapter
        extends RecyclerView.Adapter<PredictionHistoryAdapter.VH> {

    private final List<PredictionHistoryUiModel> items = new ArrayList<>();

    /**
     * Replaces the current list with {@code newItems} and refreshes the view.
     *
     * @param newItems the predictions to show (may be {@code null}, treated as empty)
     */
    public void submitList(List<PredictionHistoryUiModel> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prediction_history, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder of a single prediction row.
     */
    static class VH extends RecyclerView.ViewHolder {

        private final TextView matchLabel;
        private final TextView predictedScore;
        private final TextView status;
        private final TextView points;

        VH(@NonNull View itemView) {
            super(itemView);
            matchLabel = itemView.findViewById(R.id.text_match_label);
            predictedScore = itemView.findViewById(R.id.text_predicted_score);
            status = itemView.findViewById(R.id.text_status);
            points = itemView.findViewById(R.id.text_points);
        }

        void bind(@NonNull PredictionHistoryUiModel model) {
            matchLabel.setText(model.matchLabel);
            predictedScore.setText(model.scoreText);

            status.setText(statusLabel(model.status));
            status.setTextColor(ContextCompat.getColor(
                    itemView.getContext(), statusColor(model.status)));

            // Points are shown only once the prediction is resolved (R8.4, R8.5).
            if (model.pointsText != null) {
                points.setVisibility(View.VISIBLE);
                points.setText(model.pointsText);
            } else {
                points.setVisibility(View.GONE);
            }
        }

        private int statusLabel(PredictionHistoryUiModel.Status status) {
            switch (status) {
                case CORRECT:
                    return R.string.historial_estado_acertado;
                case WRONG:
                    return R.string.historial_estado_fallido;
                case PENDING:
                default:
                    return R.string.historial_estado_pendiente;
            }
        }

        private int statusColor(PredictionHistoryUiModel.Status status) {
            switch (status) {
                case CORRECT:
                    return R.color.success_green;
                case WRONG:
                    return R.color.error_red;
                case PENDING:
                default:
                    return R.color.text_gray;
            }
        }
    }
}
