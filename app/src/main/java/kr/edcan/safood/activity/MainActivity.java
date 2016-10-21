package kr.edcan.safood.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.nitrico.lastadapter.BR;
import com.github.nitrico.lastadapter.LastAdapter;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;

import kr.edcan.safood.R;
import kr.edcan.safood.adapter.MainSafoodAdapter;
import kr.edcan.safood.databinding.ActivityMainBinding;
import kr.edcan.safood.databinding.GroupmemoGridBinding;
import kr.edcan.safood.databinding.MainGroupmemoBinding;
import kr.edcan.safood.databinding.MainInfoBinding;
import kr.edcan.safood.databinding.MainSafoodBinding;
import kr.edcan.safood.databinding.MainSearchBinding;
import kr.edcan.safood.models.Food;
import kr.edcan.safood.models.GroupMemo;
import kr.edcan.safood.models.SafoodContentData;
import kr.edcan.safood.models.SafoodGroup;
import kr.edcan.safood.models.SafoodTitleData;
import kr.edcan.safood.utils.DataManager;
import kr.edcan.safood.utils.NetworkHelper;
import kr.edcan.safood.utils.SafoodHelper;
import kr.edcan.safood.views.CartaTagView;
import kr.edcan.safood.views.SlidingExpandableListView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    String[] titleArr = new String[]{"안전한 음식", "그룹 메모", "음식 백과", "내 정보"};

    // Helper, Utils
    SafoodHelper helper;
    DataManager manager;

    // DataBindings
    public ActivityMainBinding binding;
    public MainSearchBinding mainSearchBinding;
    public MainSafoodBinding mainSafoodBinding;

    // Widgets
    SlidingExpandableListView slidingListView;
    ViewPager pager;
    RelativeLayout mainSearchFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mainSearchBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.main_search, null, true);
        mainSafoodBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.main_safood, null, true);
        setDefault();
    }


    private void setDefault() {
        manager = new DataManager(getApplicationContext());
        if (manager.getActiveUser().second.getGroupid().equals("")) {
            startActivity(new Intent(getApplicationContext(), GroupSetActivity.class));
            finish();
        }
        helper = new SafoodHelper(getApplicationContext());
        pager = (ViewPager) findViewById(R.id.mainPager);
        binding.mainPager.setAdapter(new MainPagerAdapter(getSupportFragmentManager()));
        binding.tablayout.setupWithViewPager(binding.mainPager);
        binding.mainPager.setCurrentItem(0);
        binding.mainPager.setOffscreenPageLimit(4);
        binding.mainAppbarLaunchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), CameraActivity.class));
            }
        });
        binding.mainNavButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.mainDrawer.openDrawer(GravityCompat.START);
            }
        });
    }


    public static class MainFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "pageNumber";
        ArrayList<SafoodTitleData> titleArr = new ArrayList<>();
        ArrayList<ArrayList<SafoodContentData>> contentArr = new ArrayList<>();
        MainSafoodAdapter adapter;
        boolean hasLaunched = false;
        SlidingExpandableListView listview;
        public static MainFragment newInstance(int pageNum) {
            Bundle args = new Bundle();
            MainFragment fragment = new MainFragment();
            args.putInt(ARG_SECTION_NUMBER, pageNum);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onResume() {
            super.onResume();
            if(hasLaunched){
                Call<ArrayList<SafoodGroup>> getSafoodGroupList = NetworkHelper.getNetworkInstance().getSafoodGroupList(new DataManager(getContext()).getActiveUser().second.getApikey());
                getSafoodGroupList.enqueue(new Callback<ArrayList<SafoodGroup>>() {
                    @Override
                    public void onResponse(Call<ArrayList<SafoodGroup>> call, Response<ArrayList<SafoodGroup>> response) {
                        switch (response.code()) {
                            case 200:
                                setSafoodList(response.body(), listview, false);
                                break;
                            default:
                                break;

                        }
                    }


                    @Override
                    public void onFailure(Call<ArrayList<SafoodGroup>> call, Throwable t) {

                    }
                });
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View view = null;
            switch (getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 0:
                    view = inflater.inflate(R.layout.main_safood, container, false);
                    break;
                case 1:
                    view = inflater.inflate(R.layout.main_groupmemo, container, false);
                    break;
                case 2:
                    view = inflater.inflate(R.layout.main_foodencyclopedia, container, false);
                    break;
                case 3:
                    view = inflater.inflate(R.layout.main_info, container, false);
                    break;
            }
            setPage(view, container, getArguments().getInt(ARG_SECTION_NUMBER), inflater);
            return view;
        }

        private void setSafoodList(ArrayList<SafoodGroup> arrayList, final SlidingExpandableListView listview, boolean append) {
            hasLaunched = true;
            titleArr.clear();
            contentArr.clear();
            for (int i = 0; i < arrayList.size(); i++) {
                titleArr.add(new SafoodTitleData(arrayList.get(i).getName(), arrayList.get(i).getFoodList().size()));
                ArrayList<SafoodContentData> data = new ArrayList<>();
                for (Food f : arrayList.get(i).getFoodList()) {
                    data.add(new SafoodContentData(f.getFoodName()));
                }
                contentArr.add(data);
            }
            if (!append)
                adapter = new MainSafoodAdapter(getContext(), titleArr, contentArr);
            else adapter.notifyDataSetChanged();
            listview.setAdapter(adapter);
            listview.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                    View view = adapter.getGroupView(groupPosition, true, null, null);
                    RelativeLayout bottomIndicator = (RelativeLayout) view.findViewById(R.id.mainListGroupBottomIndicator);
                    if (listview.isGroupExpanded(groupPosition)) {
                        bottomIndicator.setVisibility(View.INVISIBLE);
                        listview.collapseGroupWithAnimation(groupPosition);
                    } else {
                        bottomIndicator.setVisibility(View.VISIBLE);
                        listview.expandGroupWithAnimation(groupPosition);
                    }
                    return true;
                }

            });
            listview.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                    Toast.makeText(getContext(), "asdf", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        private void setPage(View view, final ViewGroup container, int position, LayoutInflater inflater) {
            switch (position) {
                case 0:
                    listview = (SlidingExpandableListView) view.findViewById(R.id.mainExpandableListView);
                    view.findViewById(R.id.editSafoodGroup).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getContext(), "항목을 길게 눌러 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    Call<ArrayList<SafoodGroup>> getSafoodGroupList = NetworkHelper.getNetworkInstance().getSafoodGroupList(new DataManager(getContext()).getActiveUser().second.getApikey());
                    getSafoodGroupList.enqueue(new Callback<ArrayList<SafoodGroup>>() {
                        @Override
                        public void onResponse(Call<ArrayList<SafoodGroup>> call, Response<ArrayList<SafoodGroup>> response) {
                            switch (response.code()) {
                                case 200:
                                    setSafoodList(response.body(), listview, false);
                                    break;
                                default:
                                    break;

                            }
                        }


                        @Override
                        public void onFailure(Call<ArrayList<SafoodGroup>> call, Throwable t) {

                        }
                    });
                    CartaTagView newFolder = (CartaTagView) view.findViewById(R.id.newSafoodGroup);
                    newFolder.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.newfolder_view, null, false);
                            final EditText text = (EditText) dialogView.findViewById(R.id.editText);
                            new MaterialDialog.Builder(getContext())
                                    .title("새로운 폴더 추가")
                                    .customView(dialogView, false).positiveText("생성").onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    Call<ArrayList<SafoodGroup>> addSafoodGroup = NetworkHelper.getNetworkInstance().newSafoodGroup(
                                            text.getText().toString().trim().toString(),
                                            new DataManager(getContext()).getActiveUser().second.getApikey(),
                                            0
                                    );
                                    addSafoodGroup.enqueue(new Callback<ArrayList<SafoodGroup>>() {
                                        @Override
                                        public void onResponse(Call<ArrayList<SafoodGroup>> call, Response<ArrayList<SafoodGroup>> response) {
                                            switch (response.code()) {
                                                case 200:
                                                    Toast.makeText(getContext(), "폴더가 생성되었습니다!", Toast.LENGTH_SHORT).show();
                                                    setSafoodList(response.body(), listview, true);
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<ArrayList<SafoodGroup>> call, Throwable t) {

                                        }
                                    });
                                }
                            }).show();
                        }
                    });
                    break;
                case 1:
                    CartaTagView newMemo = (CartaTagView) view.findViewById(R.id.newMemo);
                    newMemo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startActivity(new Intent(v.getContext(), NewGroupMemoActivity.class));
                        }
                    });
                    final RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
                    final ArrayList<GroupMemo> arrayList = new ArrayList<>();
                    Call<ArrayList<GroupMemo>> getMemo = NetworkHelper.getNetworkInstance().getGroupMemo(
                            new DataManager(getContext()).getActiveUser().second.getGroupid()
                    );
                    getMemo.enqueue(new Callback<ArrayList<GroupMemo>>() {
                        @Override
                        public void onResponse(Call<ArrayList<GroupMemo>> call, Response<ArrayList<GroupMemo>> response) {
                            switch (response.code()){
                                case 200:
                                    arrayList.addAll(response.body());
                                    recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
                                    LastAdapter.with(arrayList, BR._all)
                                            .layoutHandler(new LastAdapter.LayoutHandler() {
                                                @Override
                                                public int getItemLayout(@NotNull Object o, int i) {
                                                    return R.layout.groupmemo_grid;
                                                }
                                            })
                                            .onBindListener(new LastAdapter.OnBindListener() {
                                                @Override
                                                public void onBind(@NotNull Object o, @NotNull View view, int i, int i1) {
                                                    GroupmemoGridBinding binding = DataBindingUtil.getBinding(view);
                                                    binding.title.setText(((GroupMemo) o).getTitle());
                                                    binding.content.setText(((GroupMemo) o).getContent());
                                                    binding.foods.setText(((GroupMemo) o).getFoods().size()+" 개의 음식");
                                                }
                                            })
                                            .onClickListener(new LastAdapter.OnClickListener() {
                                                @Override
                                                public void onClick(@NotNull Object o, @NotNull View view, int i, int i1) {
                                                    startActivity(new Intent(getContext(), GroupMemoActivity.class)
                                                    .putExtra("json", new Gson().toJson(o)));
                                                }
                                            })
                                            .into(recyclerView);
                                    break;
                                default:
                                    break;
                            }
                        }

                        @Override
                        public void onFailure(Call<ArrayList<GroupMemo>> call, Throwable t) {

                        }
                    });

                    break;
                case 3:
                    MainInfoBinding b = DataBindingUtil.inflate(LayoutInflater.from(getContext()),R.layout.main_info, null, false);
                    CartaTagView[] arr = {
                            b.c1,
                            b.c2, b.c3, b.c4, b.c5, b.c6, b.c7, b.c8, b.c9, b.c10, b.c11, b.c12, b.c13, b.c14, b.c15
                    };
                    for(final CartaTagView c : arr) c.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            c.setFullMode(!c.getFullMode());
                        }
                    });
                    break;
            }
        }
    }


    public class MainPagerAdapter extends FragmentPagerAdapter {


        public MainPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return MainFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < titleArr.length)
                return titleArr[position];
            return null;
        }
    }

}
