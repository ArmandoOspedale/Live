package com.live;

import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InfoFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        int position = getArguments().getInt("position");
        int comp = getArguments().getInt("comp");
        ViewGroup rootView = null;
        switch (comp) {
            case 1:
            case 4:
            case 6:
                switch (position) {
                    case 0:
                        rootView = ultima(inflater, container);
                        break;
                    case 1:
                        rootView = classifica(inflater, container);
                        break;
                    case 2:
                        rootView = prossima(inflater, container);
                        break;
                }
                break;
            case 2:
            case 3:
                rootView = classifica(inflater, container);
                break;
            default:
                switch (position) {
                    case 0:
                        rootView = ultima(inflater, container);
                        break;
                    case 1:
                        rootView = prossima(inflater, container);
                        break;
                }
                break;
        }

        return rootView;
    }

    private ViewGroup ultima (LayoutInflater inflater, ViewGroup container) {
        String [] ultima = getArguments().getStringArray("ultima");
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.lista, container, false);
        ListView p1 = rootView.findViewById(R.id.lista);
        p1.setSelector(new StateListDrawable());
        if (ultima != null) {
            if (ultima[0].equals("LA COMPETIZIONE INIZIERA':")) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.row, R.id.row, new String[]{ultima[0], ultima[1]});
                p1.setAdapter(adapter);
            } else {
                if (ultima[0].contains("/")) {
                    ArrayList<HashMap<String, String>> feedList = new ArrayList<>();
                    for (String s : ultima) {
                        String[] split = s.split("/");
                        HashMap<String, String> map = new HashMap<>();
                        map.put("casa", split[0]);
                        map.put("trasf", split[1]);
                        map.put("punteggio", split[2]);
                        map.put("result", split[3]);

                        feedList.add(map);
                    }

                    SimpleAdapter adapter1 =
                            new SimpleAdapter(getActivity(), feedList, R.layout.calenlist, new String[]{"casa", "trasf", "punteggio", "result"}, new int[]{R.id.casa, R.id.trasf, R.id.punteggio, R.id.result});
                    p1.setAdapter(adapter1);
                } else {
                    ArrayAdapter<String> adapter2 =
                            new ArrayAdapter<>(getActivity(), R.layout.row, R.id.row, ultima);
                    p1.setAdapter(adapter2);
                }
            }
        }
        return rootView;
    }

    @SuppressWarnings("unchecked")
    private ViewGroup classifica (LayoutInflater inflater, ViewGroup container) {
        List<HashMap<String, String>> result = (List<HashMap<String, String>>) getArguments().getSerializable("classifica");
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.classifica, container, false);
        if (result != null) {
            ListView list = rootView.findViewById(R.id.listView);
            list.setSelector(new StateListDrawable());

            SimpleAdapter simpleAdapter = new SimpleAdapter(getActivity(), result, R.layout.classlist, new String[]{"NomeSquadra", "Giocate", "Punti", "Totale"}, new int[]{R.id.NomeSquadra, R.id.Giocate, R.id.Punti, R.id.Totale});
            list.setAdapter(simpleAdapter);
        }
        return rootView;
    }

    private ViewGroup prossima (LayoutInflater inflater, ViewGroup container) {
        String [] prossima = getArguments().getStringArray("prossima");
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.lista, container, false);
        ListView p2 = rootView.findViewById(R.id.lista);
        p2.setSelector(new StateListDrawable());

        if (prossima != null) {
            ArrayAdapter<String> adapter2 =
                    new ArrayAdapter<>(getActivity(), R.layout.row, R.id.row, prossima);
            p2.setAdapter(adapter2);
        }
        return rootView;
    }
}