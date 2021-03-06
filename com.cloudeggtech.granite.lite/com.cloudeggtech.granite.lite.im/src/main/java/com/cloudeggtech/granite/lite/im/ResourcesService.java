package com.cloudeggtech.granite.lite.im;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.cloudeggtech.basalt.protocol.core.JabberId;
import com.cloudeggtech.basalt.protocol.im.stanza.Presence;
import com.cloudeggtech.granite.framework.im.IResource;
import com.cloudeggtech.granite.framework.im.IResourcesRegister;
import com.cloudeggtech.granite.framework.im.IResourcesService;

@Component
public class ResourcesService implements IResourcesService, IResourcesRegister {
	private ConcurrentMap<String, Resources> bareIdAndResources = new ConcurrentHashMap<>();
	
	@Override
	public IResource[] getResources(JabberId jid) {
		Resources resources = bareIdAndResources.get(jid.getBareIdString());
		if (resources == null)
			return new IResource[0];
		
		return resources.getResources();
	}
	
	@Override
	public boolean register(JabberId jid) {
		getResourcesByJid(jid).register(jid);
		
		return true;
	}
	
	@Override
	public boolean unregister(JabberId jid) {
		Resources resources = bareIdAndResources.get(jid.getBareIdString());
		if (resources != null) {
			resources.unregister(jid);
		}
		
		return true;
	}
	
	@Override
	public boolean setRosterRequested(JabberId jid) {
		getResourcesByJid(jid).setRosterRequested(jid);
		return true;
	}
	
	@Override
	public boolean setBroadcastPresence(JabberId jid, Presence presence) {
		getResourcesByJid(jid).setPresence(jid, presence);
		return true;
	}
	
	@Override
	public boolean setAvailable(JabberId jid) {
		getResourcesByJid(jid).setAvailable(jid);
		return true;
	}

	private Resources getResourcesByJid(JabberId jid) {
		String bareId = jid.getBareIdString();
		Resources resources = bareIdAndResources.get(bareId);
		if (resources == null) {
			resources = new Resources(bareId);
			Resources oldResources = bareIdAndResources.putIfAbsent(bareId, resources);
			if (oldResources != null)
				resources = oldResources;
		}
		
		return resources;
	}
	
	private Resource getResourceByJid(JabberId jid) {
		String bareId = jid.getBareIdString();
		Resources resources = bareIdAndResources.get(bareId);
		if (resources == null) {
			return null;
		}
		
		for (IResource resource : resources.getResources()) {
			if (resource.getJid().equals(jid))
				return (Resource)resource;
		}
		
		return null;
	}
	
	private class Resources {
		private String bareId;
		private List<Resource> resources;
		private boolean removed = false;
		
		public Resources(String bareId) {
			this.bareId = bareId;
			resources = new ArrayList<>();
		}
		
		public synchronized IResource[] getResources() {
			if (resources.size() == 0) {
				return new IResource[0];
			}
			
			return resources.toArray(new IResource[resources.size()]);
		}
		
		public void register(JabberId jid) {
			synchronized (this) {
				if (!removed) {
					Resource resource = new Resource(jid);
					resources.add(resource);
					return;
				}
			}
			
			ResourcesService.this.register(jid);
		}
		
		public void setRosterRequested(JabberId jid) {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						if (resource.getJid().equals(jid)) {
							resource.rosterRequested = true;
						}
					}
					
					return;
				}
			}
			
			ResourcesService.this.setRosterRequested(jid);
		}
		
		public void setPresence(JabberId jid, Presence presence) {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						if (resource.getJid().equals(jid)) {
							resource.broadcastPresence = presence;
						}
					}
					
					return;
				}
			}
			
			ResourcesService.this.setBroadcastPresence(jid, presence);
		}
		
		public void setDirectedPresence(JabberId from, Presence presence) {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						resource.directedPresences.put(from, presence);
					}
					
					return;
				}
			}
			
			ResourcesService.this.setDirectedPresence(from, JabberId.parse(bareId), presence);
		}
		
		public void setAvailable(JabberId jid) {
			synchronized (this) {
				if (!removed) {
					for (Resource resource : resources) {
						if (resource.getJid().equals(jid)) {
							resource.available = true;
						}
					}
					
					return;
				}
			}
			
			ResourcesService.this.setAvailable(jid);
		}
		
		public void unregister(JabberId jid) {
			synchronized(this) {
				if (!removed) {
					Resource toBeRemoved = null;
					for (Resource resource : resources) {
						if (jid.equals(resource.getJid())) {
							toBeRemoved  = resource;
							break;
						}
					}
					
					if (toBeRemoved != null) {
						resources.remove(toBeRemoved);
					}
					
					if (resources.size() == 0) {
						ResourcesService.this.bareIdAndResources.remove(bareId);
						removed = true;
					}
					
					return;
				}
			}
			
			ResourcesService.this.unregister(jid);
		}
		
		
	}
	
	private class Resource implements IResource {
		private JabberId jid;
		private volatile boolean rosterRequested;
		private volatile Presence broadcastPresence;
		private volatile boolean available;
		private ConcurrentMap<JabberId, Presence> directedPresences;
		
		public Resource(JabberId jid) {
			this.jid = jid;
			rosterRequested = false;
			directedPresences = new ConcurrentHashMap<>();
		}

		@Override
		public JabberId getJid() {
			return jid;
		}

		@Override
		public boolean isRosterRequested() {
			return rosterRequested;
		}

		@Override
		public Presence getBroadcastPresence() {
			return broadcastPresence;
		}
		
		@Override
		public boolean isAvailable() {
			return available;
		}

		@Override
		public Presence getDirectedPresences(JabberId from) {
			return directedPresences.get(from);
		}
	}

	@Override
	public IResource getResource(JabberId jid) {
		if (jid.getResource() == null)
			return null;
		
		IResource[] resources = getResources(jid);
		for (IResource resource : resources) {
			if (resource.getJid().equals(jid))
				return resource;
		}
		
		return null;
	}

	@Override
	public boolean setDirectedPresence(JabberId from, JabberId to, Presence presence) {
		if (to.getResource() == null) {
			getResourcesByJid(to).setDirectedPresence(from, presence);
		} else {
			Resource resource = getResourceByJid(to);
			if (resource != null) {
				resource.directedPresences.put(from, presence);
			}
		}
		
		return true;
	}

}
