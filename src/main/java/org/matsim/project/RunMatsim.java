/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.project;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * @author nagel
 *
 */
public class RunMatsim{
	;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	public static void main(String[] args) {

		Config config;

		String startTime = LocalDateTime.now().format(formatter);

		if ( args==null || args.length==0 || args[0]==null ){
			config = ConfigUtils.loadConfig( "scenarios/equil/config.xml" );
		} else {
			config = ConfigUtils.loadConfig( args );
		}

		config.controller().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );

		// possibly modify config here

		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;

		// possibly modify scenario here
		
		// ---
		
		Controler controler = new Controler( scenario ) ;
		
		// possibly modify controler here

//		controler.addOverridingModule( new OTFVisLiveModule() ) ;

//		controler.addOverridingModule( new SimWrapperModule() );
		
		// ---

		try {
			// Run the MATSim simulation
			controler.run();
		} catch (Exception e) {
			// Log any exception that occurs during the simulation
		} finally {
			// Log the end time after the simulation completes
			String endTime = LocalDateTime.now().format(formatter);
			java.time.Duration duration = java.time.Duration.between(LocalDateTime.parse(startTime, formatter), LocalDateTime.parse(endTime, formatter));
			long hours = duration.toHours();
			long minutes = duration.toMinutesPart();
			long seconds = duration.toSecondsPart();
			System.out.println("Duration: " + hours + " hours " + minutes + " minutes " + seconds + " seconds");

		}
	}
	
}
