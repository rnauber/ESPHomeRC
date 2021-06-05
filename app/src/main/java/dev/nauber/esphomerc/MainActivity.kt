package dev.nauber.esphomerc

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.materialdrawer.iconics.iconicsIcon
import com.mikepenz.materialdrawer.model.*
import com.mikepenz.materialdrawer.model.interfaces.*
import com.mikepenz.materialdrawer.util.addItems
import com.mikepenz.materialdrawer.util.setupWithNavController
import com.mikepenz.materialdrawer.widget.AccountHeaderView
import dev.nauber.esphomerc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val viewModel: ControlCommViewModel by viewModels()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        val drawerLayout: DrawerLayout? = findViewById(R.id.drawer_layout)

        val appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        val headerView = AccountHeaderView(this).apply {
            attachToSliderView(binding.slider)
            onAccountHeaderListener = { view, profile, current ->
                if (profile is IDrawerItem<*>) {                    //add vehicle
                    if (profile.identifier == ID_ADD_VEHICLE.toLong()) {
                        val newid = viewModel.newVehicleId()
                        viewModel.addVehicle(newid, context.getString(R.string.new_vehicle))
                        viewModel.currentVehicleId = newid
                        true
                    } else if (profile.identifier == ID_REMOVE_VEHICLE.toLong()) {//remove vehicle
                        if (viewModel.vehicleIds.size > 1) {
                            viewModel.removeVehicle(viewModel.currentVehicleId)
                            viewModel.currentVehicleId = viewModel.vehicleIds[0]
                        }
                        true
                    } else { // select profile
                        viewModel.currentVehicleId = profile.identifier
                        false
                    }
                } else
                //false if you have not consumed the event and it should close the drawer
                    false
            }
            withSavedInstance(savedInstanceState)
        }

        headerView.onlyMainProfileImageVisible = true
        viewModel.liveVehicleIds.observe(this, {
            headerView.clear()
            headerView.addProfiles(
                * it.map { vid ->
                    ProfileDrawerItem().apply {
                        nameText = viewModel.getVehicleSetting(vid, "name") ?: "Unnamed";
                        descriptionText = viewModel.getVehicleSetting(vid, "name") ?: "Unnamed";
                        iconicsIcon = GoogleMaterial.Icon.gmd_electric_car;
                        identifier = vid
                    }
                }.toTypedArray(),
                ProfileSettingDrawerItem().apply {
                    nameRes = R.string.add_vehicle;
                    iconDrawable = IconicsDrawable(
                        headerView.context,
                        GoogleMaterial.Icon.gmd_add
                    ).mutate();
                    isIconTinted = true;
                    identifier = ID_ADD_VEHICLE.toLong()
                },
                ProfileSettingDrawerItem().apply {
                    nameRes = R.string.remove_vehicle;
                    iconicsIcon = GoogleMaterial.Icon.gmd_remove;
                    identifier = ID_REMOVE_VEHICLE.toLong()
                }
            )
            headerView.setActiveProfile(viewModel.currentVehicleId, false)
        })

        viewModel.liveCurrentVehicleId.observe(this, {
            headerView.setActiveProfile(it, false)
        })

        binding.slider.apply {
            addItems(
                NavigationDrawerItem(
                    R.id.cockpitFragment,
                    PrimaryDrawerItem().apply { nameRes = R.string.title_cockpit }
                ),
                DividerDrawerItem(),
                NavigationDrawerItem(
                    R.id.settingsFragment,
                    PrimaryDrawerItem().apply { nameRes = R.string.title_settings }),
                NavigationDrawerItem(
                    R.id.connectionFragment,
                    PrimaryDrawerItem().apply { nameRes = R.string.title_connection }),
                NavigationDrawerItem(
                    R.id.controllerFragment,
                    PrimaryDrawerItem().apply { nameRes = R.string.title_controller })
            )
//            addStickyDrawerItems(
//            NavigationDrawerItem(R.id.aboutFragment, PrimaryDrawerItem().apply { nameRes=R.string.title_about })
            //            )
        }

        // setup the drawer with navigation controller
        binding.slider.setupWithNavController(navController)

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.reconnect -> {
                    viewModel.reconnect()
                    true
                }
                else -> false
            }
        }
    }


    companion object {
        private const val ID_ADD_VEHICLE = 100000
        private const val ID_REMOVE_VEHICLE = 100001
    }
}
