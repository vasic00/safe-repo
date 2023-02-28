package model;

import java.io.Serializable;
import java.util.HashSet;

public class User implements Serializable {
	
	private final String userName;
	private final String password;

	private final String countryName;
	private final String stateOrProvinceName;
	private final String localityName;
	private final String organizationName;
	private final String organizationalUnitName;
	private final String commonName;
	private final String emailAddress;
	
	
	public User(String userName, String password, String countryName, String stateOrProvinceName, String localityName, 
			String organizationName, String organizationalUnitName, String commonName, String emailAddress)
	{
		this.userName = userName;
		this.password = password;
		this.countryName = countryName;
		this.stateOrProvinceName = stateOrProvinceName;
		this.localityName = localityName;
		this.organizationName = organizationName;
		this.organizationalUnitName = organizationalUnitName;
		this.commonName = commonName;
		this.emailAddress = emailAddress;
		
	}
	
	public String getUserName()
	{
		return userName;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public String getCountryName()
	{
		return countryName;
	}
	
	public String getStateOrProvinceName()
	{
		return stateOrProvinceName;
	}
	
	public String getLocalityName()
	{
		return localityName;
	}
	
	public String getOrganizationName()
	{
		return organizationName;
	}
	
	public String getOrganizationalUnitName()
	{
		return organizationalUnitName;
	}
	
	public String getCommonName()
	{
		return commonName;
	}
	
	public String getEmailAddress()
	{
		return emailAddress;
	}
	
	@Override
	public String toString()
	{
		return countryName + "/" + stateOrProvinceName + "/" + localityName + "/" + organizationName + "/" + organizationalUnitName + "/" + commonName + "/" + emailAddress;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof User)
		{
			User other = (User)obj;
			return userName.equals(other.userName) && countryName.equals(other.countryName) && stateOrProvinceName.equals(other.stateOrProvinceName)
					&& localityName.equals(other.localityName) && organizationName.equals(other.organizationName)
					&& organizationalUnitName.equals(other.organizationalUnitName) && commonName.equals(other.commonName)
					&& emailAddress.equals(other.emailAddress);
		}
		return false;
	}
	
}
